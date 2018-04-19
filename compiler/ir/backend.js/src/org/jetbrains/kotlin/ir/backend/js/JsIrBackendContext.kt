/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    irModuleFragment: IrModuleFragment,
    symbolTable: SymbolTable
) : CommonBackendContext {

    val intrinsics = JsIntrinsics(module, irBuiltIns, symbolTable)

    override val builtIns = module.builtIns
    override val sharedVariablesManager = JsSharedVariablesManager(builtIns)

    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // TODO
        ReflectionTypes(module, FqName("kotlin.reflect"))
    }

    override val ir: Ir<CommonBackendContext> = object : Ir<CommonBackendContext>(this, irModuleFragment) {
        override val symbols: Symbols<CommonBackendContext> = object : Symbols<CommonBackendContext>(this@JsIrBackendContext, symbolTable) {

            override fun calc(initializer: () -> IrClassSymbol): IrClassSymbol {
                return object : IrClassSymbol {
                    override val owner: IrClass get() = TODO("not implemented")
                    override val isBound: Boolean get() = TODO("not implemented")
                    override fun bind(owner: IrClass) = TODO("not implemented")
                    override val descriptor: ClassDescriptor get() = TODO("not implemented")
                }
            }

            override val areEqual
                get () = TODO("not implemented")

            override val ThrowNullPointerException
                get () = TODO("not implemented")

            override val ThrowNoWhenBranchMatchedException
                get () = TODO("not implemented")

            override val ThrowTypeCastException
                get () = TODO("not implemented")

            override val ThrowUninitializedPropertyAccessException = symbolTable.referenceSimpleFunction(
                irBuiltIns.defineOperator(
                    "throwUninitializedPropertyAccessException",
                    builtIns.nothingType,
                    listOf(builtIns.stringType)
                ).descriptor
            )

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineImpl: IrClassSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    data class SecondaryCtorPair(val delegate: IrSimpleFunctionSymbol, val stub: IrSimpleFunctionSymbol)

    val secondaryConstructorsMap = mutableMapOf<IrConstructorSymbol, SecondaryCtorPair>()

    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        return find(memberScope, Name.identifier(className))
    }

    private fun find(memberScope: MemberScope, name: Name): ClassDescriptor {
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
    }

    override fun getInternalClass(name: String): ClassDescriptor {
        return find(module.getPackage(FqName("kotlin.js")).memberScope, name)
    }

    override fun getClass(fqName: FqName): ClassDescriptor {
        return find(module.getPackage(fqName.parent()).memberScope, fqName.shortName())
    }

    override fun getInternalFunctions(name: String): List<FunctionDescriptor> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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