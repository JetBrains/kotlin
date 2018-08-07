/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.descriptors.KnownPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.js.JsDescriptorsFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ModuleIndex
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment
) : CommonBackendContext {

    override val builtIns = module.builtIns

    override val sharedVariablesManager =
        JsSharedVariablesManager(builtIns, KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal")))
    override val descriptorsFactory = JsDescriptorsFactory()
    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // TODO
        ReflectionTypes(module, FqName("kotlin.reflect"))
    }

    private val internalPackageName = FqName("kotlin.js")
    private val internalPackage = module.getPackage(internalPackageName)

    val intrinsics = JsIntrinsics(module, irBuiltIns, this)

    private val operatorMap = referenceOperators()

    data class SecondaryCtorPair(val delegate: IrSimpleFunctionSymbol, val stub: IrSimpleFunctionSymbol)

    val secondaryConstructorsMap = mutableMapOf<IrConstructorSymbol, SecondaryCtorPair>()
    val enumEntryToGetInstanceFunction = mutableMapOf<IrEnumEntrySymbol, IrSimpleFunctionSymbol>()

    fun getOperatorByName(name: Name, type: IrType) = operatorMap[name]?.get(type.toKotlinType())

    val originalModuleIndex = ModuleIndex(irModuleFragment)

    override val ir = object : Ir<CommonBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<CommonBackendContext>(this@JsIrBackendContext, symbolTable.lazyWrapper) {

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
                get () = irBuiltIns.throwNpeSymbol

            override val ThrowNoWhenBranchMatchedException
                get () = irBuiltIns.noWhenBranchMatchedExceptionSymbol

            override val ThrowTypeCastException
                get () = irBuiltIns.throwCceSymbol

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
                get() = TODO("not implemented")
            override val coroutineImpl: IrClassSymbol
                get() = TODO("not implemented")
            override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
                get() = TODO("not implemented")
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    private fun referenceOperators() = OperatorNames.ALL.map { name ->
        // TODO to replace KotlinType with IrType we need right equals on IrType
        name to irBuiltIns.primitiveTypes.fold(mutableMapOf<KotlinType, IrFunctionSymbol>()) { m, t ->
            val function = t.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).singleOrNull()
            function?.let { m.put(t, symbolTable.referenceSimpleFunction(it)) }
            m
        }
    }.toMap()

    private fun findClass(memberScope: MemberScope, className: String) = findClass(memberScope, Name.identifier(className))

    private fun findClass(memberScope: MemberScope, name: Name) =
        memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun findFunctions(memberScope: MemberScope, className: String) =
        findFunctions(memberScope, Name.identifier(className))

    private fun findFunctions(memberScope: MemberScope, name: Name) =
        memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()

    override fun getInternalClass(name: String) = findClass(internalPackage.memberScope, name)

    override fun getClass(fqName: FqName) = findClass(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    override fun getInternalFunctions(name: String) = findFunctions(internalPackage.memberScope, name)

    override fun log(message: () -> String) {
        /*TODO*/
        print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}