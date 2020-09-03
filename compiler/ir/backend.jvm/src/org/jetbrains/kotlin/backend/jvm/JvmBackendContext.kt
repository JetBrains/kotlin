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
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.PsiErrorBuilder
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.Type

class JvmBackendContext(
    val state: GenerationState,
    val psiSourceManager: PsiSourceManager,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment,
    private val symbolTable: SymbolTable,
    val phaseConfig: PhaseConfig,
    // If the JVM fqname of a class differs from what is implied by its parent, e.g. if it's a file class
    // annotated with @JvmPackageName, the correct name is recorded here.
    val classNameOverride: MutableMap<IrClass, JvmClassName>,
    internal val createCodegen: (IrClass, JvmBackendContext, IrFunction?) -> ClassCodegen?,
) : CommonBackendContext {
    override val transformedFunction: MutableMap<IrFunctionSymbol, IrSimpleFunctionSymbol>
        get() = TODO("not implemented")

    override val extractedLocalClasses: MutableSet<IrClass> = hashSetOf()

    override val irFactory: IrFactory = IrFactoryImpl

    override val scriptMode: Boolean = false
    override val lateinitNullableFields = mutableMapOf<IrField, IrField>()

    override val builtIns = state.module.builtIns
    val typeMapper = IrTypeMapper(this)
    val methodSignatureMapper = MethodSignatureMapper(this)

    internal val innerClassesSupport = JvmInnerClassesSupport(irFactory)
    internal val cachedDeclarations = JvmCachedDeclarations(this, methodSignatureMapper, state.languageVersionSettings)

    override val mapping: Mapping = DefaultMapping()

    val psiErrorBuilder = PsiErrorBuilder(psiSourceManager, state.diagnostics)

    override val ir = JvmIr(irModuleFragment, this.symbolTable)

    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, ir.symbols, irBuiltIns, irFactory)

    val irIntrinsics by lazy { IrIntrinsicMethods(irBuiltIns, ir.symbols) }

    private val localClassType = mutableMapOf<IrAttributeContainer, Type>()

    internal fun getLocalClassType(container: IrAttributeContainer): Type? =
        localClassType[container.attributeOwnerId]

    internal fun putLocalClassType(container: IrAttributeContainer, value: Type) {
        localClassType[container.attributeOwnerId] = value
    }

    internal val isEnclosedInConstructor = mutableSetOf<IrAttributeContainer>()

    internal val classCodegens = mutableMapOf<IrClass, ClassCodegen>()

    val localDelegatedProperties = mutableMapOf<IrClass, List<IrLocalDelegatedPropertySymbol>>()

    internal val multifileFacadesToAdd = mutableMapOf<JvmClassName, MutableList<IrClass>>()
    val multifileFacadeForPart = mutableMapOf<IrClass, JvmClassName>()
    internal val multifileFacadeClassForPart = mutableMapOf<IrClass, IrClass>()
    internal val multifileFacadeMemberToPartMember = mutableMapOf<IrSimpleFunction, IrSimpleFunction>()

    internal val hiddenConstructors = mutableMapOf<IrConstructor, IrConstructor>()

    internal val collectionStubComputer = CollectionStubComputer(this)
    internal val bridgeLoweringCache = BridgeLowering.BridgeLoweringCache(this)

    override var inVerbosePhase: Boolean = false

    override val configuration get() = state.configuration

    override val internalPackageFqn = FqName("kotlin.jvm")

    val suspendLambdaToOriginalFunctionMap = mutableMapOf<IrFunctionReference, IrFunction>()
    val suspendFunctionOriginalToView = mutableMapOf<IrFunction, IrFunction>()
    val fakeContinuation: IrExpression = createFakeContinuation(this)

    val staticDefaultStubs = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunction>()

    val inlineClassReplacements = MemoizedInlineClassReplacements(state.functionsWithInlineClassReturnTypesMangled, irFactory)

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
