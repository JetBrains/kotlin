/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator.ClosureBuilder
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.MultiFieldValueClassMapping
import org.jetbrains.kotlin.backend.jvm.MemoizedMultiFieldValueClassReplacements.RemappedParameter.RegularMapping
import org.jetbrains.kotlin.backend.jvm.caches.BridgeLoweringCache
import org.jetbrains.kotlin.backend.jvm.caches.CollectionStubComputer
import org.jetbrains.kotlin.backend.jvm.extensions.JvmIrDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBackendContext(
    val state: GenerationState,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val generatorExtensions: JvmGeneratorExtensions,
    val backendExtension: JvmBackendExtension,
    val irSerializer: JvmIrSerializer?,
    val irDeserializer: JvmIrDeserializer,
    val irProviders: List<IrProvider>,
    val irPluginContext: IrPluginContext?,
) : CommonBackendContext {
    class SharedLocalDeclarationsData(
        val closureBuilders: MutableMap<IrDeclaration, ClosureBuilder> = mutableMapOf<IrDeclaration, ClosureBuilder>(),
        val transformedDeclarations: MutableMap<IrSymbolOwner, IrDeclaration> = mutableMapOf<IrSymbolOwner, IrDeclaration>(),
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf(),
        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
    )

    val config: JvmBackendConfig = state.config

    // If this is not null, the JVM IR backend is invoked in the context of Evaluate Expression in the IDE.
    var evaluatorData: JvmEvaluatorData? = null

    override val irFactory: IrFactory = IrFactoryImpl

    override val typeSystem: IrTypeSystemContext = JvmIrTypeSystemContext(irBuiltIns)
    val defaultTypeMapper = IrTypeMapper(this)
    val defaultMethodSignatureMapper = MethodSignatureMapper(this, defaultTypeMapper)

    override val innerClassesSupport: InnerClassesSupport = JvmInnerClassesSupport(irFactory)
    val cachedDeclarations = JvmCachedDeclarations(
        this, generatorExtensions.cachedFields
    )

    val allConstructorsWithCapturedConstructorCreated = mutableSetOf<IrConstructor>()

    val ktDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(state.diagnosticReporter, config.languageVersionSettings)

    override val symbols = JvmSymbols(this)

    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, symbols, irBuiltIns, irFactory)

    lateinit var getIntrinsic: (IrFunctionSymbol) -> IntrinsicMarker?

    lateinit var enumEntriesIntrinsicMappingsCache: EnumEntriesIntrinsicMappingsCache

    val isCompilingAgainstJdk8OrLater = state.jvmBackendClassResolver.resolveToClassDescriptors(
        Type.getObjectType("java/lang/invoke/LambdaMetafactory")
    ).isNotEmpty()

    val multifileFacadesToAdd = mutableMapOf<JvmClassName, MutableList<IrClass>>()

    val collectionStubComputer = CollectionStubComputer(this)

    val bridgeLoweringCache = BridgeLoweringCache(this)

    override var inVerbosePhase: Boolean = false

    override val configuration get() = state.configuration

    val inlineClassReplacements = MemoizedInlineClassReplacements(config.functionsWithInlineClassReturnTypesMangled, irFactory, this)

    val multiFieldValueClassReplacements = MemoizedMultiFieldValueClassReplacements(irFactory, this)

    val inlineMethodGenerationLock = Any()

    val optionalAnnotations = mutableListOf<MetadataSource.Class>()

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

    override val preferJavaLikeCounterLoop: Boolean
        get() = true

    override val optimizeLoopsOverUnsignedArrays: Boolean
        get() = true

    override val doWhileCounterLoopOrigin: IrStatementOrigin
        get() = JvmLoweredStatementOrigin.DO_WHILE_COUNTER_LOOP

    override val optimizeNullChecksUsingKotlinNullability: Boolean
        get() = false

    override val shouldGenerateHandlerParameterForDefaultBodyFun: Boolean
        get() = true

    override fun remapMultiFieldValueClassStructure(
        oldFunction: IrFunction,
        newFunction: IrFunction,
        parametersMappingOrNull: Map<IrValueParameter, IrValueParameter>?,
    ) {
        val parametersMapping = parametersMappingOrNull ?: run {
            require(oldFunction.parameters.size == newFunction.parameters.size) {
                "Use non-default mapping instead:\n${oldFunction.render()}\n${newFunction.render()}"
            }
            oldFunction.parameters.zip(newFunction.parameters).toMap()
        }
        val oldRemappedParameters = oldFunction.parameterTemplateStructureOfThisNewMfvcBidingFunction ?: return
        val newRemapsFromOld = oldRemappedParameters.mapNotNull { oldRemapping ->
            when (oldRemapping) {
                is RegularMapping -> parametersMapping[oldRemapping.valueParameter]?.let(::RegularMapping)
                is MultiFieldValueClassMapping -> {
                    val newParameters = oldRemapping.parameters.map { parametersMapping[it] }
                    when {
                        newParameters.all { it == null } -> null
                        newParameters.none { it == null } -> oldRemapping.copy(parameters = newParameters.map { it!! })
                        else -> error("Illegal new parameters:\n${newParameters.joinToString("\n") { it?.dump() ?: "null" }}")
                    }
                }
            }
        }
        val remappedParameters = newRemapsFromOld.flatMap { remap -> remap.parameters.map { it to remap } }.toMap()
        val newBinding = newFunction.parameters.map { remappedParameters[it] ?: RegularMapping(it) }.distinct()
        newFunction.parameterTemplateStructureOfThisNewMfvcBidingFunction = newBinding
    }
}
