/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.descriptors.KnownPackageFragmentDescriptor
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.js.JsDeclarationFactory
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceKey
import org.jetbrains.kotlin.ir.backend.js.lower.ConstructorPair
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ModuleIndex
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPropertyDeclaration
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.createDynamicType

class JsIrBackendContext(
    val module: ModuleDescriptorImpl,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment,
    override val configuration: CompilerConfiguration,
    val compilationMode: CompilationMode
) : CommonBackendContext {

    override val builtIns = module.builtIns

    val phaseConfig = PhaseConfig(jsPhases, configuration)
    override var inVerbosePhase: Boolean = false

    val externalNestedClasses = mutableListOf<IrClass>()
    val packageLevelJsModules = mutableListOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclaration>()

    val internalPackageFragmentDescriptor = KnownPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))
    val implicitDeclarationFile by lazy {
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

    companion object {
        val KOTLIN_PACKAGE_FQN = FqName.fromSegments(listOf("kotlin"))

        private val INTRINSICS_PACKAGE_NAME = Name.identifier("intrinsics")
        private val COROUTINE_SUSPENDED_NAME = Name.identifier("COROUTINE_SUSPENDED")
        private val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        private val COROUTINE_IMPL_NAME = Name.identifier("CoroutineImpl")
        private val CONTINUATION_NAME = Name.identifier("Continuation")
        // TODO: what is more clear way reference this getter?
        private val CONTINUATION_CONTEXT_GETTER_NAME = Name.special("<get-context>")

        private val CONTINUATION_CONTEXT_PROPERTY_NAME = Name.identifier("context")
        private val REFLECT_PACKAGE_FQNAME = KOTLIN_PACKAGE_FQN.child(Name.identifier("reflect"))
        private val JS_PACKAGE_FQNAME = KOTLIN_PACKAGE_FQN.child(Name.identifier("js"))
        private val JS_INTERNAL_PACKAGE_FQNAME = JS_PACKAGE_FQNAME.child(Name.identifier("internal"))
        private val COROUTINE_PACKAGE_FQNAME_12 = FqName.fromSegments(listOf("kotlin", "coroutines", "experimental"))
        private val COROUTINE_PACKAGE_FQNAME_13 = FqName.fromSegments(listOf("kotlin", "coroutines"))
        private val COROUTINE_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME_13
        private val COROUTINE_INTRINSICS_PACKAGE_FQNAME = COROUTINE_PACKAGE_FQNAME.child(INTRINSICS_PACKAGE_NAME)

        // TODO: due to name clash those weird suffix is required, remove it once `NameGenerator` is implemented
        private val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"

        val callableClosureOrigin = object : IrDeclarationOriginImpl("CALLABLE_CLOSURE_DECLARATION") {}
    }

    private val internalPackage = module.getPackage(JS_PACKAGE_FQNAME)

    private val coroutinePackage = module.getPackage(COROUTINE_PACKAGE_FQNAME)
    private val coroutineIntrinsicsPackage = module.getPackage(COROUTINE_INTRINSICS_PACKAGE_FQNAME)

    val enumEntryToGetInstanceFunction = mutableMapOf<IrEnumEntrySymbol, IrSimpleFunction>()
    val enumEntryExternalToInstanceField = mutableMapOf<IrEnumEntrySymbol, IrField>()
    val callableReferencesCache = mutableMapOf<CallableReferenceKey, IrSimpleFunction>()
    val secondaryConstructorToFactoryCache = mutableMapOf<IrConstructor, ConstructorPair>()

    val intrinsics = JsIntrinsics(irBuiltIns, this)

    private val operatorMap = referenceOperators()

    // TODO: get rid of this

    fun functionN(n: Int) = symbolTable.lazyWrapper.referenceClass(builtIns.getFunction(n))
    fun suspendFunctionN(n: Int) = symbolTable.lazyWrapper.referenceClass(builtIns.getSuspendFunction(n))

    private fun primitivesWithImplicitCompanionObject(): List<Name> {
        val numbers = PrimitiveType.NUMBER_TYPES
            .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
            .map { it.typeName }

        return numbers + listOf(Name.identifier("String"))
    }

    val dynamicType = IrDynamicTypeImpl(createDynamicType(builtIns), emptyList(), Variance.INVARIANT)

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

            override val ThrowNullPointerException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))).single())

            override val ThrowNoWhenBranchMatchedException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException"))).single())

            override val ThrowTypeCastException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))).single())

            override val ThrowUninitializedPropertyAccessException =
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented")
            override val coroutineImpl =
                symbolTable.referenceClass(findClass(coroutinePackage.memberScope, COROUTINE_IMPL_NAME.identifier))
            override val coroutineSuspendedGetter =
                symbolTable.referenceSimpleFunction(
                    coroutineIntrinsicsPackage.memberScope.getContributedVariables(
                        COROUTINE_SUSPENDED_NAME,
                        NoLookupLocation.FROM_BACKEND
                    ).filterNot { it.isExpect }.single().getter!!
                )

            override val lateinitIsInitializedPropertyGetter =
                symbolTable.referenceSimpleFunction(
                    module.getPackage(kotlinPackageFqn).memberScope.getContributedVariables(
                        Name.identifier("isInitialized"), NoLookupLocation.FROM_BACKEND
                    ).single {
                        it.extensionReceiverParameter != null && !it.isExternal
                    }.getter!!
                )
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    // classes forced to be loaded

    val primitiveClassesObject = symbolTable.referenceClass(
        getClass(FqName.fromSegments(listOf("kotlin", "reflect", "js", "internal", "PrimitiveClasses")))
    )

    val throwableClass = symbolTable.referenceClass(getClass(JsIrBackendContext.KOTLIN_PACKAGE_FQN.child(Name.identifier("Throwable"))))

    val primitiveCompanionObjects = primitivesWithImplicitCompanionObject().associate {
        it to symbolTable.referenceClass(
            getClass(JS_INTERNAL_PACKAGE_FQNAME.child(Name.identifier("${it.identifier}CompanionObject")))
        )
    }

    val coroutineImpl = ir.symbols.coroutineImpl
    val continuationClass = symbolTable.referenceClass(
        coroutinePackage.memberScope.getContributedClassifier(
            CONTINUATION_NAME,
            NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor
    )


    // Top-level functions forced to be loaded

    val coroutineSuspendOrReturn = symbolTable.referenceSimpleFunction(getInternalFunctions(COROUTINE_SUSPEND_OR_RETURN_JS_NAME).single())
    val coroutineSuspendGetter = ir.symbols.coroutineSuspendedGetter
    val coroutineGetContext: IrFunctionSymbol
        get() {
            val contextGetter =
                continuationClass.owner.declarations.filterIsInstance<IrFunction>().atMostOne { it.name == CONTINUATION_CONTEXT_GETTER_NAME }
                    ?: continuationClass.owner.declarations.filterIsInstance<IrProperty>().atMostOne { it.name == CONTINUATION_CONTEXT_PROPERTY_NAME }?.getter!!
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

    val lateinitIsInitializedPropertyGetter = ir.symbols.lateinitIsInitializedPropertyGetter

    val captureStackSymbol = symbolTable.referenceSimpleFunction(getInternalFunctions("captureStack").single())
    val newThrowableSymbol = symbolTable.referenceSimpleFunction(getInternalFunctions("newThrowable").single())

    val throwISEymbol = symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))).single())
    val throwNPESymbol = ir.symbols.ThrowNullPointerException
    val throwCCESymbol = ir.symbols.ThrowTypeCastException
    val throwNWBESymbol = ir.symbols.ThrowNoWhenBranchMatchedException
    val throwUPAESymbol = ir.symbols.ThrowUninitializedPropertyAccessException

    val coroutineImplLabelProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("state")!! }
    val coroutineImplResultSymbol by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("result")!! }
    val coroutineImplExceptionProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("exception")!! }
    val coroutineImplExceptionStateProperty by lazy { ir.symbols.coroutineImpl.getPropertyDeclaration("exceptionState")!! }

    val primitiveClassProperties by lazy {
        primitiveClassesObject.owner.declarations.filterIsInstance<IrProperty>()
    }

    val primitiveClassFunctionClass by lazy {
        primitiveClassesObject.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .find { it.name == Name.identifier("functionClass") }!!
    }

    val throwableConstructors by lazy { throwableClass.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol } }
    val defaultThrowableCtor by lazy { throwableConstructors.single { it.owner.valueParameters.size == 0 } }

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
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}
