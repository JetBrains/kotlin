/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.js.JsDeclarationFactory
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.SourceManager
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsSharedVariablesManager
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceKey
import org.jetbrains.kotlin.ir.backend.js.lower.ConstructorPair
import org.jetbrains.kotlin.ir.backend.js.utils.OperatorNames
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.Variance

class WasmBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    irModuleFragment: IrModuleFragment,
    val additionalExportedDeclarations: Set<FqName>,
    override val configuration: CompilerConfiguration
) : CommonBackendContext {

    override val builtIns = module.builtIns

    override var inVerbosePhase: Boolean = false

    var externalPackageFragment = mutableMapOf<FqName, IrPackageFragment>()
    lateinit var bodilessBuiltInsPackageFragment: IrPackageFragment

    val externalNestedClasses = mutableListOf<IrClass>()
    val packageLevelJsModules = mutableListOf<IrFile>()
    val declarationLevelJsModules = mutableListOf<IrDeclarationWithName>()

    val internalPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, FqName("kotlin.js.internal"))
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

    override val sharedVariablesManager = JsSharedVariablesManager(irBuiltIns, implicitDeclarationFile)
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

    val enumEntryToGetInstanceFunction = mutableMapOf<IrEnumEntrySymbol, IrSimpleFunction>()
    val objectToGetInstanceFunction = mutableMapOf<IrClassSymbol, IrSimpleFunction>()
    val enumEntryExternalToInstanceField = mutableMapOf<IrEnumEntrySymbol, IrField>()
    val callableReferencesCache = mutableMapOf<CallableReferenceKey, IrSimpleFunction>()
    val secondaryConstructorToFactoryCache = mutableMapOf<IrConstructor, ConstructorPair>()

    override val internalPackageFqn = JS_PACKAGE_FQNAME

    private val operatorMap = referenceOperators()

    private fun primitivesWithImplicitCompanionObject(): List<Name> {
        val numbers = PrimitiveType.NUMBER_TYPES
            .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
            .map { it.typeName }

        return numbers + listOf(Name.identifier("String"), Name.identifier("Boolean"))
    }

    fun getOperatorByName(name: Name, type: IrSimpleType) = operatorMap[name]?.get(type.classifier)

    override val ir = object : Ir<WasmBackendContext>(this, irModuleFragment) {
        override val symbols = object : Symbols<WasmBackendContext>(this@WasmBackendContext, symbolTable.lazyWrapper) {
            override val ThrowNullPointerException by lazy { TODO() }
                // symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_NPE"))).single())

            override val ThrowNoWhenBranchMatchedException by lazy { TODO() }
                // symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("noWhenBranchMatchedException"))).single())

            override val ThrowTypeCastException by lazy { TODO() }
                // symbolTable.referenceSimpleFunction(getFunctions(kotlinPackageFqn.child(Name.identifier("THROW_CCE"))).single())

            override val ThrowUninitializedPropertyAccessException by lazy { TODO() }
                // symbolTable.referenceSimpleFunction(getFunctions(FqName("kotlin.throwUninitializedPropertyAccessException")).single())

            override val defaultConstructorMarker by lazy { TODO() }
                // symbolTable.referenceClass(context.getJsInternalClass("DefaultConstructorMarker"))

            override val stringBuilder
                get() = TODO("not implemented")
            override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
                get() = TODO("not implemented")
            override val coroutineImpl by lazy { TODO() }
                // symbolTable.referenceClass(findClass(coroutinePackage.memberScope, COROUTINE_IMPL_NAME))

            override val coroutineSuspendedGetter by lazy { TODO() }

            override val getContinuation by lazy { TODO() } //
                //    = symbolTable.referenceSimpleFunction(getJsInternalFunction("getContinuation"))

            override val coroutineContextGetter by lazy { TODO() } // = symbolTable.referenceSimpleFunction(context.coroutineContextProperty.getter!!)

            override val suspendCoroutineUninterceptedOrReturn by lazy { TODO() } // = symbolTable.referenceSimpleFunction(getJsInternalFunction(COROUTINE_SUSPEND_OR_RETURN_JS_NAME))

            override val coroutineGetContext by lazy { TODO() } // = symbolTable.referenceSimpleFunction(getJsInternalFunction(GET_COROUTINE_CONTEXT_NAME))

            override val returnIfSuspended by lazy { TODO() } // = symbolTable.referenceSimpleFunction(getJsInternalFunction("returnIfSuspended"))
        }

        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

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
        findFunctions(internalPackage.memberScope, Name.identifier(name)).single()

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
