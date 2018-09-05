/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmDeclarationFactory
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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

class JvmBackendContext(
    val state: GenerationState,
    val psiSourceManager: PsiSourceManager,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment, symbolTable: SymbolTable
) : CommonBackendContext {
    override val builtIns = state.module.builtIns
    override val declarationFactory: JvmDeclarationFactory = JvmDeclarationFactory(psiSourceManager, builtIns, state)
    override val sharedVariablesManager = JvmSharedVariablesManager(builtIns, irBuiltIns)

    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ReflectionTypes(state.module, FqName("kotlin.reflect.jvm.internal"))
    }

    override val ir = JvmIr(irModuleFragment, symbolTable)

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
        print(message())
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

            val lambdaClass = calc { symbolTable.referenceClass(context.getInternalClass("Lambda")) }
        }


        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }
}
