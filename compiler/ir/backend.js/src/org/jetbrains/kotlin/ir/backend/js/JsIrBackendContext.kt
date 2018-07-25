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
import org.jetbrains.kotlin.backend.js.JsDeclarationFactory
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ModuleIndex
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.createDynamicType

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment
) : CommonBackendContext {

    override val builtIns = module.builtIns

    val internalPackageFragmentDescriptor = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))
    val implicitDeclarationFile = IrFileImpl(object : SourceManager.FileEntry {
        override val name = "<implicitDeclarations>"
        override val maxOffset = UNDEFINED_OFFSET

        override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
            SourceRangeInfo("", UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET, UNDEFINED_OFFSET)

        override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
        override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
    }, internalPackageFragmentDescriptor).also {
        irModuleFragment.files += it
    }

    override val sharedVariablesManager =
        JsSharedVariablesManager(irBuiltIns, implicitDeclarationFile)
    override val declarationFactory = JsDeclarationFactory()
    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // TODO
        ReflectionTypes(module, FqName("kotlin.reflect"))
    }

    private val internalPackageName = FqName("kotlin.js")
    private val internalPackage = module.getPackage(internalPackageName)

    // TODO: replace it with appropriate package name once we migrate to 1.3 coroutines
    private val coroutinePackageNameSrting = "kotlin.coroutines.experimental"

    private val INTRINSICS_PACKAGE_NAME = Name.identifier("intrinsics")
    private val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")
    private val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
    private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
    private val CONTINUATION_NAME = Name.identifier("Continuation")

    // TODO: what is more clear way reference this getter?
    private val CONTINUATION_CONTEXT_GETTER_NAME = Name.special("<get-context>")

    private val coroutinePackageName = FqName(coroutinePackageNameSrting)
    private val coroutineIntrinsicsPackageName = coroutinePackageName.child(INTRINSICS_PACKAGE_NAME)

    private val coroutinePackage = module.getPackage(coroutinePackageName)
    private val coroutineIntrinsicsPackage = module.getPackage(coroutineIntrinsicsPackageName)

    val enumEntryToGetInstanceFunction = mutableMapOf<IrEnumEntrySymbol, IrSimpleFunctionSymbol>()

    val coroutineGetContext: IrFunctionSymbol
        get() {
            val continuation = symbolTable.referenceClass(
                coroutinePackage.memberScope.getContributedClassifier(
                    CONTINUATION_NAME,
                    NoLookupLocation.FROM_BACKEND
                ) as ClassDescriptor
            )
            val contextGetter = continuation.owner.declarations.single { it.descriptor.name == CONTINUATION_CONTEXT_GETTER_NAME } as IrFunction
            return contextGetter.symbol
        }

    val coroutineContextProperty: PropertyDescriptor
        get() {
            val vars = internalPackage.memberScope.getContributedVariables(
                COROUTINE_CONTEXT_NAME,
                NoLookupLocation.FROM_BACKEND
            )
            return vars.single()
        }

    val intrinsics = JsIntrinsics(irBuiltIns, this)

    private val operatorMap = referenceOperators()

    val functions = (0..22).map { symbolTable.referenceClass(builtIns.getFunction(it)) }

    val kFunctions by lazy {
        (0..22).map { symbolTable.referenceClass(reflectionTypes.getKFunction(it)) }
    }

    val primitiveCompanionObjects = PrimitiveType.NUMBER_TYPES
        .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
        .map {
            it.typeName to symbolTable.lazyWrapper.referenceClass(
                getClass(
                    internalPackageName
                        .child(Name.identifier("internal"))
                        .child(Name.identifier("${it.typeName.identifier}CompanionObject"))
                )
            )
        }.toMap()

    val suspendFunctions = (0..22).map { symbolTable.referenceClass(builtIns.getSuspendFunction(it)) }

    val dynamicType = IrDynamicTypeImpl(createDynamicType(builtIns), emptyList(), Variance.INVARIANT)

    val originalModuleIndex = ModuleIndex(irModuleFragment)

    fun getOperatorByName(name: Name, type: KotlinType) = operatorMap[name]?.get(type)

    override val ir = object : Ir<CommonBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<CommonBackendContext>(this@JsIrBackendContext, symbolTable.lazyWrapper) {

            override fun calc(initializer: () -> IrClassSymbol): IrClassSymbol {
                val v = lazy { initializer() }
                return object : IrClassSymbol {
                    override val owner: IrClass get() = v.value.owner
                    override val isBound: Boolean get() = v.value.isBound
                    override fun bind(owner: IrClass) = v.value.bind(owner)
                    override val descriptor: ClassDescriptor get() = v.value.descriptor
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
            override val coroutineImpl = symbolTable.referenceClass(getInternalClass(COROUTINE_IMPL_NAME.identifier))
            override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
                coroutineIntrinsicsPackage.memberScope.getContributedVariables(COROUTINE_SUSPENDED_NAME, NoLookupLocation.FROM_BACKEND).single().getter!!
            )
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

    fun getFunctions(fqName: FqName) = findFunctions(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    override fun log(message: () -> String) {
        /*TODO*/
        print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}