/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.CompilerPhases
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDeclarationFactory
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class JvmBackendContext(
    val state: GenerationState,
    val psiSourceManager: PsiSourceManager,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment, symbolTable: SymbolTable
) : CommonBackendContext {
    override val builtIns = state.module.builtIns
    override val declarationFactory: JvmDeclarationFactory = JvmDeclarationFactory(state, symbolTable)
    override val sharedVariablesManager = JvmSharedVariablesManager(builtIns, irBuiltIns)

    // TODO: inject a correct StorageManager instance, or store NotFoundClasses inside ModuleDescriptor
    internal val reflectionTypes = ReflectionTypes(state.module, NotFoundClasses(LockBasedStorageManager.NO_LOCKS, state.module))

    override val ir = JvmIr(irModuleFragment, symbolTable)

    val phases = CompilerPhases(jvmPhases, state.configuration)

    init {
        if (state.configuration.get(CommonConfigurationKeys.LIST_PHASES) == true) {
            phases.list()
        }
    }

    var inVerbosePhase = false

    fun rootPhaseManager(irFile: IrFile) = CompilerPhaseManager(this, phases, irFile, JvmPhaseRunner)


    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        return find(memberScope, Name.identifier(className))
    }

    private fun find(memberScope: MemberScope, name: Name): ClassDescriptor {
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    }

    override fun getInternalClass(name: String): ClassDescriptor {
        return find(state.module.getPackage(FqName("kotlin.jvm.internal")).memberScope, name)
    }

    override fun getClass(fqName: FqName): ClassDescriptor {
        return find(state.module.getPackage(fqName.parent()).memberScope, fqName.shortName())
    }

    fun getIrClass(fqName: FqName): IrClassSymbol {
        return ir.symbols.externalSymbolTable.referenceClass(getClass(fqName))
    }

    override fun getInternalFunctions(name: String): List<FunctionDescriptor> {
        return when (name) {
            "ThrowUninitializedPropertyAccessException" ->
                getInternalClass("Intrinsics").staticScope.getContributedFunctions(
                    Name.identifier("throwUninitializedPropertyAccessException"),
                    NoLookupLocation.FROM_BACKEND
                ).toList()
            else -> TODO(name)
        }
    }

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
        private val symbolTable: SymbolTable
    ) : Ir<JvmBackendContext>(this, irModuleFragment) {
        override val symbols = JvmSymbols()

        inner class JvmSymbols : Symbols<JvmBackendContext>(this@JvmBackendContext, symbolTable.lazyWrapper) {

            override val areEqual
                get () = symbolTable.referenceSimpleFunction(context.getInternalFunctions("areEqual").single())

            override val ThrowNullPointerException
                get () = symbolTable.referenceSimpleFunction(
                    context.getInternalFunctions("ThrowNullPointerException").single()
                )

            override val ThrowNoWhenBranchMatchedException
                get () = symbolTable.referenceSimpleFunction(
                    context.getInternalFunctions("ThrowNoWhenBranchMatchedException").single()
                )

            override val ThrowTypeCastException
                get () = symbolTable.referenceSimpleFunction(
                    context.getInternalFunctions("ThrowTypeCastException").single()
                )

            override val ThrowUninitializedPropertyAccessException =
                symbolTable.referenceSimpleFunction(
                    context.getInternalFunctions("ThrowUninitializedPropertyAccessException").single()
                )

            override val stringBuilder
                get() = symbolTable.referenceClass(
                    context.getClass(FqName("java.lang.StringBuilder"))
                )

            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineImpl: IrClassSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

            override val lateinitIsInitializedPropertyGetter= symbolTable.referenceSimpleFunction(
                state.module.getPackage(FqName("kotlin")).memberScope.getContributedVariables(
                    Name.identifier("isInitialized"), NoLookupLocation.FROM_BACKEND
                ).single {
                    it.extensionReceiverParameter != null && !it.isExternal
                }.getter!!
            )

            val lambdaClass = calc { symbolTable.referenceClass(context.getInternalClass("Lambda")) }

            fun getKFunction(parameterCount: Int) = symbolTable.referenceClass(reflectionTypes.getKFunction(parameterCount))
        }


        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }
}
