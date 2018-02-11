/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmSharedVariablesManager
import org.jetbrains.kotlin.backend.jvm.descriptors.SpecialDescriptorsFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class JvmBackendContext(
        val state: GenerationState,
        psiSourceManager: PsiSourceManager,
        override val irBuiltIns: IrBuiltIns,
        irModuleFragment: IrModuleFragment, symbolTable: SymbolTable
) : CommonBackendContext {
    override val builtIns = state.module.builtIns
    val specialDescriptorsFactory = SpecialDescriptorsFactory(psiSourceManager, builtIns)
    override val sharedVariablesManager = JvmSharedVariablesManager(builtIns)

    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ReflectionTypes(state.module, FqName("kotlin.reflect.jvm.internal"))
    }

    override val ir: Ir<CommonBackendContext> = object : Ir<CommonBackendContext>(this, irModuleFragment) {
        override val symbols: Symbols<CommonBackendContext> =  object: Symbols<CommonBackendContext>(this@JvmBackendContext, symbolTable) {

            override val areEqual
                get () = symbolTable.referenceSimpleFunction(context.getInternalFunctions("areEqual").single())

            override val ThrowNullPointerException
                get () = symbolTable.referenceSimpleFunction(
                        context.getInternalFunctions("ThrowNullPointerException").single())

            override val ThrowNoWhenBranchMatchedException
                get () = symbolTable.referenceSimpleFunction(
                        context.getInternalFunctions("ThrowNoWhenBranchMatchedException").single())

            override val ThrowTypeCastException
                get () = symbolTable.referenceSimpleFunction(
                        context.getInternalFunctions("ThrowTypeCastException").single())

            override val ThrowUninitializedPropertyAccessException
                get () = symbolTable.referenceSimpleFunction(
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
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

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
                getInternalClass("Intrinsics").staticScope.
                        getContributedFunctions(Name.identifier("throwUninitializedPropertyAccessException"), NoLookupLocation.FROM_BACKEND).toList()
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
}
