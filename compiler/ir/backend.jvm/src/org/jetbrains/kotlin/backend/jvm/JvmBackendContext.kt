/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.InlineClassesUtils
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.fileForTopLevelPluginDeclarations
import org.jetbrains.kotlin.backend.common.lower.ClosureAnnotator.ClosureBuilder
import org.jetbrains.kotlin.backend.common.lower.InnerClassesSupport
import org.jetbrains.kotlin.backend.jvm.caches.BridgeLoweringCache
import org.jetbrains.kotlin.backend.jvm.caches.CollectionStubComputer
import org.jetbrains.kotlin.backend.jvm.mapping.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.mapping.MethodSignatureMapper
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrBuiltIns
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
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JvmBackendContext(
    val state: GenerationState,
    override val irBuiltIns: IrBuiltIns,
    irModule: IrModuleFragment,
    val symbolTable: SymbolTable,
    val debuggerExtensions: JvmDebuggerExtensions?,
    val backendExtension: JvmBackendExtension,
    val irPluginContext: IrPluginContext?,
    val evaluatorData: JvmEvaluatorData?,
) : CommonBackendContext {
    class SharedLocalDeclarationsData(
        val closureBuilders: MutableMap<IrDeclaration, ClosureBuilder> = mutableMapOf<IrDeclaration, ClosureBuilder>(),
        val transformedDeclarations: MutableMap<IrSymbolOwner, IrDeclaration> = mutableMapOf<IrSymbolOwner, IrDeclaration>(),
        val newParameterToCaptured: MutableMap<IrValueParameter, IrValueSymbol> = mutableMapOf(),
        val newParameterToOld: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
        val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf(),
    )

    val config: JvmBackendConfig = state.config

    override val irFactory: IrFactory = IrFactoryImpl

    val specialAnnotationsProvider: JvmIrSpecialAnnotationSymbolProvider =
        JvmIrSpecialAnnotationSymbolProvider()
    override val typeSystem: IrTypeSystemContext = JvmIrTypeSystemContext(irBuiltIns)
    val defaultTypeMapper = IrTypeMapper(this)
    val defaultMethodSignatureMapper = MethodSignatureMapper(this, defaultTypeMapper)

    override val innerClassesSupport: InnerClassesSupport = JvmInnerClassesSupport(irFactory)
    val cachedDeclarations = JvmCachedDeclarations(
        this, CachedFieldsForObjectInstances(IrFactoryImpl)
    )

    override val diagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(state.diagnosticReporter, config.languageVersionSettings)

    override val symbols = JvmSymbols(this, irModule)

    override val sharedVariablesManager = JvmSharedVariablesManager(symbols, irBuiltIns, irModule, irFactory)

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

    val inlineMethodGenerationLock = Any()

    val optionalAnnotations = mutableListOf<MetadataSource.Class>()

    @Deprecated("It is non-JVM API", level = DeprecationLevel.ERROR)
    override val inlineClassesUtils: InlineClassesUtils
        get() = error("Not supported in JVM")

    init {
        state.mapInlineClass = { descriptor ->
            defaultTypeMapper.mapType(referenceClass(descriptor).defaultType)
        }

        state.reportDuplicateClassNameError = { class1, internalName, class2 ->
            diagnosticReporter.at(class1).report(
                JvmBackendErrors.DUPLICATE_CLASS_NAMES, internalName,
                listOf(class1, class2).joinToString { it.name.asString() },
            )
        }

        state.isDeclarationGeneratedForCompilerPlugin = { declaration ->
            declaration.fileOrNull?.fileForTopLevelPluginDeclarations == true
        }
    }

    fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol =
        symbolTable.lazyWrapper.descriptorExtension.referenceClass(descriptor)

    fun referenceTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol =
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
}
