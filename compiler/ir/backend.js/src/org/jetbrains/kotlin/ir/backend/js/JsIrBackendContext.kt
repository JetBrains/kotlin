/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ReflectionTypes
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.descriptors.KnownPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.js.JsDeclarationFactory
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrEnumEntrySymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPropertyDeclaration
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
    irModuleFragment: IrModuleFragment,
    val configuration: CompilerConfiguration
) : CommonBackendContext {

    override val builtIns = module.builtIns

    val internalPackageFragmentDescriptor = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))
    private val implicitDeclarationFile by lazy {
        IrFileImpl(object : SourceManager.FileEntry {
            override val name = "<implicitDeclarations>"
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_OFFSET
            override fun getColumnNumber(offset: Int) = UNDEFINED_OFFSET
        }, internalPackageFragmentDescriptor).also {
            irModuleFragment.files += it
        }
    }

    override val sharedVariablesManager =
        JsSharedVariablesManager(irBuiltIns, implicitDeclarationFile)
    override val declarationFactory = JsDeclarationFactory()
    override val reflectionTypes: ReflectionTypes by lazy(LazyThreadSafetyMode.PUBLICATION) {
        // TODO
        ReflectionTypes(module, REFLECT_PACKAGE_FQNAME)
    }

    companion object {
        private val INTRINSICS_PACKAGE_NAME = Name.identifier("intrinsics")
        private val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")
        private val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
        private val CONTINUATION_NAME = Name.identifier("Continuation")
        // TODO: what is more clear way reference this getter?
        private val CONTINUATION_CONTEXT_GETTER_NAME = Name.special("<get-context>")
        private val CONTINUATION_CONTEXT_PROPERTY_NAME = Name.identifier("context")

        private val REFLECT_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "reflect"))
        private val JS_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "js"))
        private val JS_INTERNAL_PACKAGE_FQNAME = JS_PACKAGE_FQNAME.child(Name.identifier("internal"))
        private val COROUTINE_PACKAGE_FQNAME_12 = FqName.fromSegments(listOf("kotlin", "coroutines", "experimental"))
        private val COROUTINE_PACKAGE_FQNAME_13 = FqName.fromSegments(listOf("kotlin", "coroutines"))
        private val COROUTINE_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME_13
        private val COROUTINE_INTRINSICS_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME.child(INTRINSICS_PACKAGE_NAME)

        // TODO: due to name clash those weird suffix is required, remove it once `NameGenerator` is implemented
        private val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
    }

    private val internalPackage = module.getPackage(JS_PACKAGE_FQNAME)

    private val coroutinePackage = module.getPackage(COROUTINE_PACKAGE_FQNAME)
    private val coroutineIntrinsicsPackage = module.getPackage(COROUTINE_INTRINSICS_PACKAGE_FQNAME)

    val enumEntryToGetInstanceFunction = mutableMapOf<IrEnumEntrySymbol, IrSimpleFunctionSymbol>()

    val coroutineGetContext: IrFunctionSymbol
        get() {
            val continuation = symbolTable.referenceClass(
                coroutinePackage.memberScope.getContributedClassifier(
                    CONTINUATION_NAME,
                    NoLookupLocation.FROM_BACKEND
                ) as ClassDescriptor
            )
            val contextGetter =
                continuation.owner.declarations.filterIsInstance<IrFunction>().atMostOne { it.descriptor.name == CONTINUATION_CONTEXT_GETTER_NAME }
                    ?: continuation.owner.declarations.filterIsInstance<IrProperty>().atMostOne { it.descriptor.name == CONTINUATION_CONTEXT_PROPERTY_NAME }?.getter!!
            return contextGetter.symbol
        }

    val coroutineGetContextJs = symbolTable.referenceSimpleFunction(getInternalFunctions(GET_COROUTINE_CONTEXT_NAME).single())

    val coroutineContextProperty: PropertyDescriptor
        get() {
            val vars = coroutinePackage.memberScope.getContributedVariables(
                COROUTINE_CONTEXT_NAME,
                NoLookupLocation.FROM_BACKEND
            )
            return vars.single()
        }

    val coroutineSuspendOrReturn =
        symbolTable.referenceSimpleFunction(getInternalFunctions(COROUTINE_SUSPEND_OR_RETURN_JS_NAME).single())

    val intrinsics = JsIntrinsics(irBuiltIns, this)

    private val operatorMap = referenceOperators()

    val functions = (0..22).map { symbolTable.referenceClass(builtIns.getFunction(it)) }

    val kFunctions by lazy {
        (0..22).map { symbolTable.referenceClass(reflectionTypes.getKFunction(it)) }
    }

    val primitiveCompanionObjects = PrimitiveType.NUMBER_TYPES
        .asSequence()
        .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
        .map {
            it.typeName to symbolTable.lazyWrapper.referenceClass(
                getClass(JS_INTERNAL_PACKAGE_FQNAME.child(Name.identifier("${it.typeName.identifier}CompanionObject")))
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

            override val ThrowUninitializedPropertyAccessException=
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented")
            override val coroutineImpl = symbolTable.referenceClass(findClass(coroutinePackage.memberScope, COROUTINE_IMPL_NAME.identifier))
            override val coroutineSuspendedGetter = symbolTable.referenceSimpleFunction(
                coroutineIntrinsicsPackage.memberScope.getContributedVariables(
                    COROUTINE_SUSPENDED_NAME,
                    NoLookupLocation.FROM_BACKEND
                ).filterNot { it.isExpect }.single().getter!!
            )
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    val coroutineImplLabelProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("state")!! }
    val coroutineImplResultSymbol by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("result")!! }
    val coroutineImplExceptionProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("exception")!! }
    val coroutineImplExceptionStateProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("exceptionState")!! }

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