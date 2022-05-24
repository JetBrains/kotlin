/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.caches.BridgeLoweringCache
import org.jetbrains.kotlin.backend.jvm.caches.CollectionStubComputer
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type
import java.util.concurrent.ConcurrentHashMap

class JvmBackendContext(
    val state: GenerationState,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment,
    val symbolTable: SymbolTable,
    val phaseConfig: PhaseConfig,
    val generatorExtensions: JvmGeneratorExtensions,
    val backendExtension: JvmBackendExtension,
    val irSerializer: JvmIrSerializer?,
) : CommonBackendContext {

    data class LocalFunctionData(
        val localContext: LocalDeclarationsLowering.LocalFunctionContext,
        val newParameterToOld: Map<IrValueParameter, IrValueParameter>,
        val newParameterToCaptured: Map<IrValueParameter, IrValueSymbol>
    )

    // If not-null, this is populated by LocalDeclarationsLowering with the intermediate data
    // allowing mapping from local function captures to parameters and accurate transformation
    // of calls to local functions from code fragments (i.e. the expression evaluator).
    var localDeclarationsLoweringData: MutableMap<IrFunction, LocalFunctionData>? = null

    // If the JVM fqname of a class differs from what is implied by its parent, e.g. if it's a file class
    // annotated with @JvmPackageName, the correct name is recorded here.
    val classNameOverride: MutableMap<IrClass, JvmClassName>
        get() = generatorExtensions.classNameOverride

    override val irFactory: IrFactory = IrFactoryImpl

    override val scriptMode: Boolean = false

    override val builtIns = state.module.builtIns
    override val typeSystem: IrTypeSystemContext = JvmIrTypeSystemContext(irBuiltIns)
    val typeMapper = IrTypeMapper(this)
    val methodSignatureMapper = MethodSignatureMapper(this)

    val innerClassesSupport = JvmInnerClassesSupport(irFactory)
    val cachedDeclarations = JvmCachedDeclarations(
        this, generatorExtensions.cachedFields
    )

    override val mapping: Mapping = DefaultMapping()

    val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(state.diagnosticReporter, state.languageVersionSettings)

    override val ir = JvmIr(irModuleFragment, this.symbolTable)

    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, ir.symbols, irBuiltIns, irFactory)

    lateinit var getIntrinsic: (IrFunctionSymbol) -> IntrinsicMarker?

    private val localClassType = ConcurrentHashMap<IrAttributeContainer, Type>()

    fun getLocalClassType(container: IrAttributeContainer): Type? =
        localClassType[container.attributeOwnerId]

    fun putLocalClassType(container: IrAttributeContainer, value: Type) {
        localClassType[container.attributeOwnerId] = value
    }

    val isEnclosedInConstructor = ConcurrentHashMap.newKeySet<IrAttributeContainer>()
    val enclosingMethodOverride = ConcurrentHashMap<IrFunction, IrFunction>()

    private val classCodegens = ConcurrentHashMap<IrClass, Any>()

    @Suppress("UNCHECKED_CAST")
    fun <ClassCodegen : Any> getOrCreateClassCodegen(klass: IrClass, create: (IrClass) -> ClassCodegen): ClassCodegen =
        classCodegens.computeIfAbsent(klass, create) as ClassCodegen

    val localDelegatedProperties = ConcurrentHashMap<IrAttributeContainer, List<IrLocalDelegatedPropertySymbol>>()

    val multifileFacadesToAdd = mutableMapOf<JvmClassName, MutableList<IrClass>>()
    val multifileFacadeForPart = mutableMapOf<IrClass, JvmClassName>()
    val multifileFacadeClassForPart = mutableMapOf<IrClass, IrClass>()
    val multifileFacadeMemberToPartMember = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    val hiddenConstructors = ConcurrentHashMap<IrConstructor, IrConstructor>()

    val collectionStubComputer = CollectionStubComputer(this)

    private val overridesWithoutStubs = HashMap<IrSimpleFunction, List<IrSimpleFunctionSymbol>>()

    fun recordOverridesWithoutStubs(function: IrSimpleFunction) {
        overridesWithoutStubs[function] = function.overriddenSymbols.toList()
    }

    fun getOverridesWithoutStubs(function: IrSimpleFunction): List<IrSimpleFunctionSymbol> =
        overridesWithoutStubs.getOrElse(function) { function.overriddenSymbols }

    val bridgeLoweringCache = BridgeLoweringCache(this)
    val functionsWithSpecialBridges: MutableSet<IrFunction> = ConcurrentHashMap.newKeySet()

    override var inVerbosePhase: Boolean = false // TODO: needs parallelizing

    override val configuration get() = state.configuration

    override val internalPackageFqn = FqName("kotlin.jvm")

    val suspendLambdaToOriginalFunctionMap = ConcurrentHashMap<IrFunctionReference, IrFunction>()
    val suspendFunctionOriginalToView = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()

    val staticDefaultStubs = ConcurrentHashMap<IrSimpleFunctionSymbol, IrSimpleFunction>()

    val inlineClassReplacements = MemoizedInlineClassReplacements(state.functionsWithInlineClassReturnTypesMangled, irFactory, this)

    val multiFieldValueClassReplacements = MemoizedMultiFieldValueClassReplacements(irFactory, this)

    val continuationClassesVarsCountByType: MutableMap<IrAttributeContainer, Map<Type, Int>> = hashMapOf()

    val inlineMethodGenerationLock = Any()

    val directInvokedLambdas = mutableListOf<IrAttributeContainer>()

    val publicAbiSymbols = mutableSetOf<IrClassSymbol>()

    init {
        state.mapInlineClass = { descriptor ->
            typeMapper.mapType(referenceClass(descriptor).defaultType)
        }
    }

    internal fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol =
        symbolTable.lazyWrapper.referenceClass(descriptor)

    internal fun referenceTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol =
        symbolTable.lazyWrapper.referenceTypeParameter(descriptor)

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) {
            print(message())
        }
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }

    override fun throwUninitializedPropertyAccessException(builder: IrBuilderWithScope, name: String): IrExpression =
        builder.irBlock {
            +super.throwUninitializedPropertyAccessException(builder, name)
        }

    override fun handleDeepCopy(
        fileSymbolMap: MutableMap<IrFileSymbol, IrFileSymbol>,
        classSymbolMap: MutableMap<IrClassSymbol, IrClassSymbol>,
        functionSymbolMap: MutableMap<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>
    ) {
        val oldClassesWithNameOverride = classNameOverride.keys.toList()
        for (klass in oldClassesWithNameOverride) {
            classSymbolMap[klass.symbol]?.let { newSymbol ->
                classNameOverride[newSymbol.owner] = classNameOverride[klass]!!
            }
        }
        for (multifileFacade in multifileFacadesToAdd) {
            val oldPartClasses = multifileFacade.value
            val newPartClasses = oldPartClasses.map { classSymbolMap[it.symbol]?.owner ?: it }
            multifileFacade.setValue(newPartClasses.toMutableList())
        }

        for ((staticReplacement, original) in inlineClassReplacements.originalFunctionForStaticReplacement) {
            if (staticReplacement !is IrSimpleFunction) continue
            val newOriginal = functionSymbolMap[original.symbol]?.owner ?: continue
            val newStaticReplacement = inlineClassReplacements.getReplacementFunction(newOriginal) ?: continue
            functionSymbolMap[staticReplacement.symbol] = newStaticReplacement.symbol
        }

        for ((methodReplacement, original) in inlineClassReplacements.originalFunctionForMethodReplacement) {
            if (methodReplacement !is IrSimpleFunction) continue
            val newOriginal = functionSymbolMap[original.symbol]?.owner ?: continue
            val newMethodReplacement = inlineClassReplacements.getReplacementFunction(newOriginal) ?: continue
            functionSymbolMap[methodReplacement.symbol] = newMethodReplacement.symbol
        }

        for ((original, suspendView) in suspendFunctionOriginalToView) {
            val newOriginal = functionSymbolMap[original.symbol]?.owner ?: continue
            val newSuspendView = suspendFunctionOriginalToView[newOriginal] ?: continue
            functionSymbolMap[suspendView.symbol] = newSuspendView.symbol
        }

        for ((nonStaticDefaultSymbol, staticDefault) in staticDefaultStubs) {
            val staticDefaultSymbol = staticDefault.symbol
            val newNonStaticDefaultSymbol = functionSymbolMap[nonStaticDefaultSymbol] ?: continue
            val newStaticDefaultSymbol = staticDefaultStubs[newNonStaticDefaultSymbol]?.symbol ?: continue
            functionSymbolMap[staticDefaultSymbol] = newStaticDefaultSymbol
        }

        super.handleDeepCopy(fileSymbolMap, classSymbolMap, functionSymbolMap)
    }

    override val preferJavaLikeCounterLoop: Boolean
        get() = true

    override val optimizeLoopsOverUnsignedArrays: Boolean
        get() = true

    override val doWhileCounterLoopOrigin: IrStatementOrigin
        get() = JvmLoweredStatementOrigin.DO_WHILE_COUNTER_LOOP

    override val optimizeNullChecksUsingKotlinNullability: Boolean
        get() = false

    inner class JvmIr(
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable
    ) : Ir<JvmBackendContext>(this, irModuleFragment) {
        override val symbols = JvmSymbols(this@JvmBackendContext, symbolTable)

        override fun unfoldInlineClassType(irType: IrType): IrType? {
            return InlineClassAbi.unboxType(irType)
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }
}
