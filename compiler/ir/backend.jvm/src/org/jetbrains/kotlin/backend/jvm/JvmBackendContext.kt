/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.psi.PsiErrorBuilder
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.codegen.MethodSignatureMapper
import org.jetbrains.kotlin.backend.jvm.codegen.createFakeContinuation
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.lower.BridgeLowering
import org.jetbrains.kotlin.backend.jvm.lower.CollectionStubComputer
import org.jetbrains.kotlin.backend.jvm.lower.JvmInnerClassesSupport
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.MemoizedInlineClassReplacements
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
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
    val notifyCodegenStart: () -> Unit,
) : CommonBackendContext {
    // If the JVM fqname of a class differs from what is implied by its parent, e.g. if it's a file class
    // annotated with @JvmPackageName, the correct name is recorded here.
    val classNameOverride: MutableMap<IrClass, JvmClassName>
        get() = generatorExtensions.classNameOverride

    override val irFactory: IrFactory = IrFactoryImpl

    override val scriptMode: Boolean = false

    override val builtIns = state.module.builtIns
    val typeMapper = IrTypeMapper(this)
    val methodSignatureMapper = MethodSignatureMapper(this)

    internal val innerClassesSupport = JvmInnerClassesSupport(irFactory)
    internal val cachedDeclarations = JvmCachedDeclarations(this, state.languageVersionSettings)

    override val mapping: Mapping = DefaultMapping()

    val psiErrorBuilder = PsiErrorBuilder(state.diagnostics)

    override val ir = JvmIr(irModuleFragment, this.symbolTable)

    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, ir.symbols, irBuiltIns, irFactory)

    val irIntrinsics by lazy { IrIntrinsicMethods(irBuiltIns, ir.symbols) }

    private val localClassType = ConcurrentHashMap<IrAttributeContainer, Type>()

    internal fun getLocalClassType(container: IrAttributeContainer): Type? =
        localClassType[container.attributeOwnerId]

    internal fun putLocalClassType(container: IrAttributeContainer, value: Type) {
        localClassType[container.attributeOwnerId] = value
    }

    internal val isEnclosedInConstructor = ConcurrentHashMap.newKeySet<IrAttributeContainer>()

    internal val classCodegens = ConcurrentHashMap<IrClass, ClassCodegen>()

    val localDelegatedProperties = ConcurrentHashMap<IrAttributeContainer, List<IrLocalDelegatedPropertySymbol>>()

    internal val multifileFacadesToAdd = mutableMapOf<JvmClassName, MutableList<IrClass>>()
    val multifileFacadeForPart = mutableMapOf<IrClass, JvmClassName>()
    internal val multifileFacadeClassForPart = mutableMapOf<IrClass, IrClass>()
    internal val multifileFacadeMemberToPartMember = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    internal val hiddenConstructors = ConcurrentHashMap<IrConstructor, IrConstructor>()

    internal val collectionStubComputer = CollectionStubComputer(this)

    private val overridesWithoutStubs = HashMap<IrSimpleFunction, List<IrSimpleFunctionSymbol>>()

    fun recordOverridesWithoutStubs(function: IrSimpleFunction) {
        overridesWithoutStubs[function] = function.overriddenSymbols.toList()
    }

    fun getOverridesWithoutStubs(function: IrSimpleFunction): List<IrSimpleFunctionSymbol> =
        overridesWithoutStubs.getOrElse(function) { function.overriddenSymbols }

    internal val bridgeLoweringCache = BridgeLowering.BridgeLoweringCache(this)
    internal val functionsWithSpecialBridges: MutableSet<IrFunction> = ConcurrentHashMap.newKeySet()

    override var inVerbosePhase: Boolean = false // TODO: needs parallelizing

    override val configuration get() = state.configuration

    override val internalPackageFqn = FqName("kotlin.jvm")

    val suspendLambdaToOriginalFunctionMap = ConcurrentHashMap<IrFunctionReference, IrFunction>()
    val suspendFunctionOriginalToView = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()
    val fakeContinuation: IrExpression = createFakeContinuation(this)

    val staticDefaultStubs = ConcurrentHashMap<IrSimpleFunctionSymbol, IrSimpleFunction>()

    val inlineClassReplacements = MemoizedInlineClassReplacements(state.functionsWithInlineClassReturnTypesMangled, irFactory, this)

    internal val continuationClassesVarsCountByType: MutableMap<IrAttributeContainer, Map<Type, Int>> = hashMapOf()

    val inlineMethodGenerationLock = Any()

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
            +irThrow(irNull())
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
