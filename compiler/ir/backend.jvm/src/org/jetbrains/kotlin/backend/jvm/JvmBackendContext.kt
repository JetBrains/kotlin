/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DefaultMapping
import org.jetbrains.kotlin.backend.common.Mapping
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.RegularMapping
import org.jetbrains.kotlin.backend.jvm.caches.BridgeLoweringCache
import org.jetbrains.kotlin.backend.jvm.caches.CollectionStubComputer
import org.jetbrains.kotlin.backend.jvm.extensions.JvmIrDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmBackendErrors
import org.jetbrains.org.objectweb.asm.Type
import java.util.concurrent.ConcurrentHashMap

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBackendContext(
    val state: GenerationState,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val phaseConfig: PhaseConfig,
    val generatorExtensions: JvmGeneratorExtensions,
    val backendExtension: JvmBackendExtension,
    val irSerializer: JvmIrSerializer?,
    val irDeserializer: JvmIrDeserializer,
    val irProviders: List<IrProvider>,
    val irPluginContext: IrPluginContext?,
) : CommonBackendContext {

    @Suppress("UNUSED_PARAMETER")
    @Deprecated("irModuleFragment parameter is not needed anymore.", level = DeprecationLevel.ERROR)
    constructor(
        state: GenerationState,
        irBuiltIns: IrBuiltIns,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        phaseConfig: PhaseConfig,
        generatorExtensions: JvmGeneratorExtensions,
        backendExtension: JvmBackendExtension,
        irSerializer: JvmIrSerializer?,
        irProviders: List<IrProvider>,
        irDeserializer: JvmIrDeserializer,
    ) : this(
        state, irBuiltIns, symbolTable, phaseConfig, generatorExtensions,
        backendExtension, irSerializer, irDeserializer, irProviders, irPluginContext = null
    )

    data class LocalFunctionData(
        val localContext: LocalDeclarationsLowering.LocalFunctionContext,
        val newParameterToOld: Map<IrValueParameter, IrValueParameter>,
        val newParameterToCaptured: Map<IrValueParameter, IrValueSymbol>,
    )

    val config: JvmBackendConfig = state.config

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
    val defaultTypeMapper = IrTypeMapper(this)
    val defaultMethodSignatureMapper = MethodSignatureMapper(this, defaultTypeMapper)

    val innerClassesSupport = JvmInnerClassesSupport(irFactory)
    val cachedDeclarations = JvmCachedDeclarations(
        this, generatorExtensions.cachedFields
    )

    override val mapping: Mapping = DefaultMapping()

    val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(state.diagnosticReporter, state.languageVersionSettings)

    override val ir = JvmIr(this.symbolTable)

    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, ir.symbols, irBuiltIns, irFactory)

    lateinit var getIntrinsic: (IrFunctionSymbol) -> IntrinsicMarker?

    lateinit var enumEntriesIntrinsicMappingsCache: EnumEntriesIntrinsicMappingsCache

    // Store evaluated SMAP for anonymous classes. Used only with IR inliner.
    val typeToCachedSMAP = mutableMapOf<Type, SMAP>()

    private val localClassType = ConcurrentHashMap<IrAttributeContainer, Type>()

    val isCompilingAgainstJdk8OrLater = state.jvmBackendClassResolver.resolveToClassDescriptors(
        Type.getObjectType("java/lang/invoke/LambdaMetafactory")
    ).isNotEmpty()

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

    val hiddenConstructorsWithMangledParams = ConcurrentHashMap<IrConstructor, IrConstructor>()
    val hiddenConstructorsOfSealedClasses = ConcurrentHashMap<IrConstructor, IrConstructor>()

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

    val suspendLambdaToOriginalFunctionMap = ConcurrentHashMap<IrAttributeContainer, IrFunction>()
    val suspendFunctionOriginalToView = ConcurrentHashMap<IrSimpleFunction, IrSimpleFunction>()

    val staticDefaultStubs = ConcurrentHashMap<IrSimpleFunctionSymbol, IrSimpleFunction>()

    val inlineClassReplacements = MemoizedInlineClassReplacements(config.functionsWithInlineClassReturnTypesMangled, irFactory, this)

    val multiFieldValueClassReplacements = MemoizedMultiFieldValueClassReplacements(irFactory, this)

    val continuationClassesVarsCountByType: MutableMap<IrAttributeContainer, Map<Type, Int>> = hashMapOf()

    val inlineMethodGenerationLock = Any()

    val publicAbiSymbols = mutableSetOf<IrClassSymbol>()

    val visitedDeclarationsForRegenerationLowering: MutableSet<IrDeclaration> = ConcurrentHashMap.newKeySet()

    init {
        state.mapInlineClass = { descriptor ->
            defaultTypeMapper.mapType(referenceClass(descriptor).defaultType)
        }

        state.multiFieldValueClassUnboxInfo = lambda@{ descriptor ->
            val irClass = referenceClass(descriptor).owner
            val node = multiFieldValueClassReplacements.getRootMfvcNodeOrNull(irClass) ?: return@lambda null
            val leavesInfo =
                node.leaves.map { Triple(defaultTypeMapper.mapType(it.type), it.fullMethodName.asString(), it.fullFieldName.asString()) }
            GenerationState.MultiFieldValueClassUnboxInfo(leavesInfo)
        }

        state.reportDuplicateClassNameError = { origin, internalName, duplicateClasses ->
            val declaration = (origin as JvmIrDeclarationOrigin).declaration
            if (declaration != null) {
                ktDiagnosticReporter.at(declaration).report(JvmBackendErrors.DUPLICATE_CLASS_NAMES, internalName, duplicateClasses)
            }
        }
    }

    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol =
        symbolTable.lazyWrapper.descriptorExtension.referenceClass(descriptor)

    internal fun referenceTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol =
        symbolTable.lazyWrapper.descriptorExtension.referenceTypeParameter(descriptor)

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
        functionSymbolMap: MutableMap<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>,
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

        for ((staticReplacement, original) in multiFieldValueClassReplacements.originalFunctionForStaticReplacement) {
            if (staticReplacement !is IrSimpleFunction) continue
            val newOriginal = functionSymbolMap[original.symbol]?.owner ?: continue
            val newStaticReplacement = multiFieldValueClassReplacements.getReplacementFunction(newOriginal) ?: continue
            functionSymbolMap[staticReplacement.symbol] = newStaticReplacement.symbol
        }

        for ((methodReplacement, original) in multiFieldValueClassReplacements.originalFunctionForMethodReplacement) {
            if (methodReplacement !is IrSimpleFunction) continue
            val newOriginal = functionSymbolMap[original.symbol]?.owner ?: continue
            val newMethodReplacement = multiFieldValueClassReplacements.getReplacementFunction(newOriginal) ?: continue
            functionSymbolMap[methodReplacement.symbol] = newMethodReplacement.symbol
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
        symbolTable: SymbolTable,
    ) : Ir<JvmBackendContext>(this) {
        override val symbols = JvmSymbols(this@JvmBackendContext, symbolTable)

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    override fun remapMultiFieldValueClassStructure(
        oldFunction: IrFunction,
        newFunction: IrFunction,
        parametersMappingOrNull: Map<IrValueParameter, IrValueParameter>?,
    ) {
        val parametersMapping = parametersMappingOrNull ?: run {
            require(oldFunction.explicitParametersCount == newFunction.explicitParametersCount) {
                "Use non-default mapping instead:\n${oldFunction.render()}\n${newFunction.render()}"
            }
            oldFunction.explicitParameters.zip(newFunction.explicitParameters).toMap()
        }
        val oldRemappedParameters = multiFieldValueClassReplacements.bindingNewFunctionToParameterTemplateStructure[oldFunction] ?: return
        val newRemapsFromOld = oldRemappedParameters.mapNotNull { oldRemapping ->
            when (oldRemapping) {
                is RegularMapping -> parametersMapping[oldRemapping.valueParameter]?.let(::RegularMapping)
                is MultiFieldValueClassMapping -> {
                    val newParameters = oldRemapping.valueParameters.map { parametersMapping[it] }
                    when {
                        newParameters.all { it == null } -> null
                        newParameters.none { it == null } -> oldRemapping.copy(valueParameters = newParameters.map { it!! })
                        else -> error("Illegal new parameters:\n${newParameters.joinToString("\n") { it?.dump() ?: "null" }}")
                    }
                }
            }
        }
        val remappedParameters = newRemapsFromOld.flatMap { remap -> remap.valueParameters.map { it to remap } }.toMap()
        val newBinding = newFunction.explicitParameters.map { remappedParameters[it] ?: RegularMapping(it) }.distinct()
        multiFieldValueClassReplacements.bindingNewFunctionToParameterTemplateStructure[newFunction] = newBinding
    }
}
