/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.IrTypeMapper
import org.jetbrains.kotlin.backend.jvm.codegen.MethodSignatureMapper
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDeclarationFactory
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrLocalDelegatedPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class JvmBackendContext(
    val state: GenerationState,
    val psiSourceManager: PsiSourceManager,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment,
    symbolTable: SymbolTable,
    val phaseConfig: PhaseConfig,
    private val firMode: Boolean
) : CommonBackendContext {
    override val builtIns = state.module.builtIns
    val typeMapper = IrTypeMapper(this)
    val methodSignatureMapper = MethodSignatureMapper(this)

    override val declarationFactory: JvmDeclarationFactory = JvmDeclarationFactory(methodSignatureMapper)
    override val sharedVariablesManager = JvmSharedVariablesManager(state.module, builtIns, irBuiltIns)

    private val symbolTable = symbolTable.lazyWrapper
    override val ir = JvmIr(irModuleFragment, this.symbolTable)

    val irIntrinsics = IrIntrinsicMethods(irBuiltIns, ir.symbols)

    // TODO: also store info for EnclosingMethod
    internal class LocalClassInfo(val internalName: String)

    private val localClassInfo = mutableMapOf<IrAttributeContainer, LocalClassInfo>()

    internal fun getLocalClassInfo(container: IrAttributeContainer): LocalClassInfo? =
        localClassInfo[container.attributeOwnerId]

    internal fun putLocalClassInfo(container: IrAttributeContainer, value: LocalClassInfo) {
        localClassInfo[container.attributeOwnerId] = value
    }

    internal val localDelegatedProperties = mutableMapOf<IrClass, List<IrLocalDelegatedPropertySymbol>>()

    internal val multifileFacadesToAdd = mutableMapOf<JvmClassName, MutableList<IrClass>>()
    internal val multifileFacadeForPart = mutableMapOf<IrClass, JvmClassName>()
    internal val multifileFacadeMemberToPartMember = mutableMapOf<IrFunctionSymbol, IrFunctionSymbol>()

    override var inVerbosePhase: Boolean = false

    override val configuration get() = state.configuration

    override val internalPackageFqn = FqName("kotlin.jvm")

    val suspendFunctionContinuations = mutableMapOf<IrFunction, IrClass>()
    val suspendLambdaToOriginalFunctionMap = mutableMapOf<IrClass, IrFunction>()
    val continuationClassBuilders = mutableMapOf<IrClass, ClassBuilder>()

    val staticDefaultStubs = mutableMapOf<IrFunctionSymbol, IrFunction>()

    internal fun getTopLevelClass(fqName: FqName): IrClassSymbol {
        val descriptor = state.module.getPackage(fqName.parent()).memberScope.getContributedClassifier(
            fqName.shortName(), NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor? ?: error("Class is not found: $fqName")
        return referenceClass(descriptor)
    }

    internal fun referenceClass(descriptor: ClassDescriptor): IrClassSymbol =
        symbolTable.referenceClass(descriptor)

    internal fun referenceTypeParameter(descriptor: TypeParameterDescriptor): IrTypeParameterSymbol =
        symbolTable.referenceTypeParameter(descriptor)

    internal fun referenceFunction(descriptor: FunctionDescriptor): IrFunctionSymbol =
        if (descriptor is ClassConstructorDescriptor)
            symbolTable.referenceConstructor(descriptor)
        else
            symbolTable.referenceSimpleFunction(descriptor)

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

    inner class JvmIr(
        irModuleFragment: IrModuleFragment,
        symbolTable: ReferenceSymbolTable
    ) : Ir<JvmBackendContext>(this, irModuleFragment) {
        override val symbols = JvmSymbols(this@JvmBackendContext, symbolTable, firMode)

        override fun unfoldInlineClassType(irType: IrType): IrType? {
            return InlineClassAbi.unboxType(irType)
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }
}
