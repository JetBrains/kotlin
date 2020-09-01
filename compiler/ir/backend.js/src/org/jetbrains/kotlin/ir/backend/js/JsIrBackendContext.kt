/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance

class JsIrBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment,
    val additionalExportedDeclarations: Set<FqName>,
    override val configuration: CompilerConfiguration, // TODO: remove configuration from backend context
    override val scriptMode: Boolean = false,
    override val es6mode: Boolean = false
) : JsCommonBackendContext {
    override val transformedFunction
        get() = error("Use Mapping.inlineClassMemberToStatic instead")

    override val lateinitNullableFields
        get() = error("Use Mapping.lateInitFieldToNullableField instead")

    override val extractedLocalClasses: MutableSet<IrClass> = hashSetOf()

    override val builtIns = module.builtIns

    override var inVerbosePhase: Boolean = false

    override val irFactory: IrFactory = PersistentIrFactory

    val devMode = configuration[JSConfigurationKeys.DEVELOPER_MODE] ?: false
    val errorPolicy = configuration[JSConfigurationKeys.ERROR_TOLERANCE_POLICY] ?: ErrorTolerancePolicy.DEFAULT

    val externalPackageFragment = mutableMapOf<IrFileSymbol, IrFile>()
    val externalDeclarations = hashSetOf<IrDeclaration>()

    val bodilessBuiltInsPackageFragment: IrPackageFragment = IrExternalPackageFragmentImpl(
        DescriptorlessExternalPackageFragmentSymbol(),
        FqName("kotlin")
    )

    val packageLevelJsModules = mutableSetOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclarationWithName>()

    private val internalPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))
    val implicitDeclarationFile = run {
        syntheticFile("implicitDeclarations", irModuleFragment)
    }

    private fun syntheticFile(name: String, module: IrModuleFragment): IrFile {
        return IrFileImpl(object : SourceManager.FileEntry {
            override val name = "<$name>"
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
            module.files += it
        }
    }

    private val testContainerFuns = mutableMapOf<IrModuleFragment, IrSimpleFunction>()

    fun createTestContainerFun(module: IrModuleFragment): IrSimpleFunction {
        return testContainerFuns.getOrPut(module) {
            val file = syntheticFile("tests", module)
            irFactory.addFunction(file) {
                name = Name.identifier("test fun")
                returnType = irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())
            }
        }
    }

    val testRoots: Map<IrModuleFragment, IrSimpleFunction>
        get() = testContainerFuns

    override val mapping = JsMapping()
    val innerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

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

        // TODO: due to name clash those weird suffix is required, remove it once `MemberNameGenerator` is implemented
        private val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
        private val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"

        val callableClosureOrigin = object : IrDeclarationOriginImpl("CALLABLE_CLOSURE_DECLARATION") {}
    }

    private val internalPackage = module.getPackage(JS_PACKAGE_FQNAME)

    private val coroutinePackage = module.getPackage(COROUTINE_PACKAGE_FQNAME)
    private val coroutineIntrinsicsPackage = module.getPackage(COROUTINE_INTRINSICS_PACKAGE_FQNAME)

    val dynamicType: IrDynamicType = IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
    val intrinsics = JsIntrinsics(irBuiltIns, this)

    override val sharedVariablesManager = JsSharedVariablesManager(this)

    override val internalPackageFqn = JS_PACKAGE_FQNAME

    private val operatorMap = referenceOperators()

    private fun primitivesWithImplicitCompanionObject(): List<Name> {
        val numbers = PrimitiveType.NUMBER_TYPES
            .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
            .map { it.typeName }

        return numbers + listOf(Name.identifier("String"), Name.identifier("Boolean"))
    }

    fun getOperatorByName(name: Name, type: IrSimpleType) = operatorMap[name]?.get(type.classifier)

    override val ir = object : Ir<JsIrBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<JsIrBackendContext>(this@JsIrBackendContext, irBuiltIns, symbolTable) {
            override val throwNullPointerException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))).single())

            override val throwNoWhenBranchMatchedException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException"))).single())

            override val throwTypeCastException =
                symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))).single())

            override val throwUninitializedPropertyAccessException =
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

            override val throwKotlinNothingValueException: IrSimpleFunctionSymbol =
                symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwKotlinNothingValueException")).single())

            override val defaultConstructorMarker =
                symbolTable.referenceClass(context.getJsInternalClass("DefaultConstructorMarker"))

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented")
            override val coroutineImpl =
                symbolTable.referenceClass(findClass(coroutinePackage.memberScope, COROUTINE_IMPL_NAME))
            override val coroutineSuspendedGetter =
                symbolTable.referenceSimpleFunction(
                    coroutineIntrinsicsPackage.memberScope.getContributedVariables(
                        COROUTINE_SUSPENDED_NAME,
                        NoLookupLocation.FROM_BACKEND
                    ).filterNot { it.isExpect }.single().getter!!
                )

            override val getContinuation = symbolTable.referenceSimpleFunction(getJsInternalFunction("getContinuation"))

            override val coroutineContextGetter = symbolTable.referenceSimpleFunction(context.coroutineContextProperty.getter!!)

            override val suspendCoroutineUninterceptedOrReturn =
                symbolTable.referenceSimpleFunction(getJsInternalFunction(COROUTINE_SUSPEND_OR_RETURN_JS_NAME))

            override val coroutineGetContext = symbolTable.referenceSimpleFunction(getJsInternalFunction(GET_COROUTINE_CONTEXT_NAME))

            override val returnIfSuspended = symbolTable.referenceSimpleFunction(getJsInternalFunction("returnIfSuspended"))

            override val functionAdapter: IrClassSymbol
                get() = TODO("Not implemented")
        }

        override fun unfoldInlineClassType(irType: IrType): IrType? {
            return irType.getInlinedClass()?.typeWith()
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    // classes forced to be loaded

    val errorCodeSymbol: IrSimpleFunctionSymbol? =
        if (errorPolicy.allowErrors) symbolTable.referenceSimpleFunction(getJsInternalFunction("errorCode")) else null

    val primitiveClassesObject = getIrClass(FqName("kotlin.reflect.js.internal.PrimitiveClasses"))

    val throwableClass = getIrClass(JsIrBackendContext.KOTLIN_PACKAGE_FQN.child(Name.identifier("Throwable")))

    val primitiveCompanionObjects = primitivesWithImplicitCompanionObject().associateWith {
        getIrClass(JS_INTERNAL_PACKAGE_FQNAME.child(Name.identifier("${it.identifier}CompanionObject")))
    }

    val coroutineImpl = ir.symbols.coroutineImpl
    val continuationClass = symbolTable.referenceClass(
        coroutinePackage.memberScope.getContributedClassifier(
            CONTINUATION_NAME,
            NoLookupLocation.FROM_BACKEND
        ) as ClassDescriptor
    )


    // Top-level functions forced to be loaded

    val coroutineSuspendOrReturn = ir.symbols.suspendCoroutineUninterceptedOrReturn
    val coroutineSuspendGetter = ir.symbols.coroutineSuspendedGetter
    val coroutineGetContext: IrSimpleFunctionSymbol
        get() {
            val contextGetter =
                continuationClass.owner.declarations.filterIsInstance<IrSimpleFunction>().atMostOne { it.name == CONTINUATION_CONTEXT_GETTER_NAME }
                    ?: continuationClass.owner.declarations.filterIsInstance<IrProperty>().atMostOne { it.name == CONTINUATION_CONTEXT_PROPERTY_NAME }?.getter!!
            return contextGetter.symbol
        }

    val coroutineGetContextJs
        get() = ir.symbols.coroutineGetContext

    val coroutineEmptyContinuation = symbolTable.referenceProperty(getProperty(FqName.fromSegments(listOf("kotlin", "coroutines", "js", "internal", "EmptyContinuation"))))

    val coroutineContextProperty: PropertyDescriptor
        get() {
            val vars = coroutinePackage.memberScope.getContributedVariables(
                COROUTINE_CONTEXT_NAME,
                NoLookupLocation.FROM_BACKEND
            )
            return vars.single()
        }

    val newThrowableSymbol = symbolTable.referenceSimpleFunction(getJsInternalFunction("newThrowable"))
    val extendThrowableSymbol = symbolTable.referenceSimpleFunction(getJsInternalFunction("extendThrowable"))
    val setPropertiesToThrowableInstanceSymbol = symbolTable.referenceSimpleFunction(getJsInternalFunction("setPropertiesToThrowableInstance"))

    val throwISEsymbol = symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_ISE"))).single())
    val throwIAEsymbol = symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_IAE"))).single())

    val suiteFun = getFunctions(FqName("kotlin.test.suite")).singleOrNull()?.let { symbolTable.referenceSimpleFunction(it) }
    val testFun = getFunctions(FqName("kotlin.test.test")).singleOrNull()?.let { symbolTable.referenceSimpleFunction(it) }

    val coroutineImplLabelPropertyGetter by lazy2 { ir.symbols.coroutineImpl.getPropertyGetter("state")!!.owner }
    val coroutineImplLabelPropertySetter by lazy2 { ir.symbols.coroutineImpl.getPropertySetter("state")!!.owner }
    val coroutineImplResultSymbolGetter by lazy2 { ir.symbols.coroutineImpl.getPropertyGetter("result")!!.owner }
    val coroutineImplResultSymbolSetter by lazy2 { ir.symbols.coroutineImpl.getPropertySetter("result")!!.owner }
    val coroutineImplExceptionPropertyGetter by lazy2 { ir.symbols.coroutineImpl.getPropertyGetter("exception")!!.owner }
    val coroutineImplExceptionPropertySetter by lazy2 { ir.symbols.coroutineImpl.getPropertySetter("exception")!!.owner }
    val coroutineImplExceptionStatePropertyGetter by lazy2 { ir.symbols.coroutineImpl.getPropertyGetter("exceptionState")!!.owner }
    val coroutineImplExceptionStatePropertySetter by lazy2 { ir.symbols.coroutineImpl.getPropertySetter("exceptionState")!!.owner }

    val primitiveClassProperties by lazy2 {
        primitiveClassesObject.owner.declarations.filterIsInstance<IrProperty>()
    }

    val primitiveClassFunctionClass by lazy2 {
        primitiveClassesObject.owner.declarations
            .filterIsInstance<IrSimpleFunction>()
            .find { it.name == Name.identifier("functionClass") }!!
    }

    val throwableConstructors by lazy2 { throwableClass.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol } }
    val defaultThrowableCtor by lazy2 { throwableConstructors.single { !it.owner.isPrimary && it.owner.valueParameters.size == 0 } }

    val kpropertyBuilder = getFunctions(FqName("kotlin.js.getPropertyCallableRef")).single().let { symbolTable.referenceSimpleFunction(it) }
    val klocalDelegateBuilder = getFunctions(FqName("kotlin.js.getLocalDelegateReference")).single().let { symbolTable.referenceSimpleFunction(it) }

    private fun referenceOperators(): Map<Name, MutableMap<IrClassifierSymbol, IrSimpleFunctionSymbol>> {
        val primitiveIrSymbols = irBuiltIns.primitiveIrTypes.map { it.classifierOrFail as IrClassSymbol }

        return OperatorNames.ALL.map { name ->
            // TODO to replace KotlinType with IrType we need right equals on IrType
            name to primitiveIrSymbols.fold(mutableMapOf<IrClassifierSymbol, IrSimpleFunctionSymbol>()) { m, s ->
                val function = s.owner.declarations.filterIsInstance<IrSimpleFunction>().singleOrNull { it.name == name }
                function?.let { m.put(s, it.symbol) }
                m
            }
        }.toMap()
    }

    private fun findClass(memberScope: MemberScope, name: Name): ClassDescriptor =
        memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun findFunctions(memberScope: MemberScope, name: Name): List<SimpleFunctionDescriptor> =
        memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()

    private fun findProperty(memberScope: MemberScope, name: Name): List<PropertyDescriptor> =
        memberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND).toList()

    internal fun getJsInternalClass(name: String): ClassDescriptor =
        findClass(internalPackage.memberScope, Name.identifier(name))

    internal fun getClass(fqName: FqName): ClassDescriptor =
        findClass(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    internal fun getProperty(fqName: FqName): PropertyDescriptor =
        findProperty(module.getPackage(fqName.parent()).memberScope, fqName.shortName()).single()

    private fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.referenceClass(getClass(fqName))

    internal fun getJsInternalFunction(name: String): SimpleFunctionDescriptor =
        findFunctions(internalPackage.memberScope, Name.identifier(name)).singleOrNull() ?: error("Internal function '$name' not found")

    fun getFunctions(fqName: FqName): List<SimpleFunctionDescriptor> =
        findFunctions(module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }
}

// TODO: investigate if it could be removed
fun <T> lazy2(fn: () -> T) = lazy { stageController.withInitialIr(fn) }