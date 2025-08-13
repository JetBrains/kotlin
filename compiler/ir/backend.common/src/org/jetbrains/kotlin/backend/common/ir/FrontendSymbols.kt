/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.*

abstract class BaseSymbolsImpl(val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder

    // TODO KT-79436 unify backend specific functions and remove the old ones
    // JS
    companion object {
        val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))
    }
    protected fun getInternalJsFunction(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), BASE_JS_PACKAGE).single()

    protected fun getInternalJsInRootPackage(name: String): IrSimpleFunctionSymbol? =
        symbolFinder.findFunctions(Name.identifier(name), FqName.ROOT).singleOrNull()

    // WASM
    protected val enumsInternalPackageFqName = FqName("kotlin.enums")
    protected val wasmInternalFqName = FqName("kotlin.wasm.internal")
    protected fun getFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol {
        return maybeGetFunction(name, ownerPackage) ?: throw IllegalArgumentException("Function $name not found")
    }

    protected fun maybeGetFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol? {
        return symbolFinder.topLevelFunctions(ownerPackage, name).singleOrNull()
    }

    protected fun getInternalWasmFunction(name: String): IrSimpleFunctionSymbol = getFunction(name, wasmInternalFqName)

    protected fun getEnumsFunction(name: String) = getFunction(name, enumsInternalPackageFqName)

    protected fun getIrClassOrNull(fqName: FqName): IrClassSymbol? = symbolFinder.findClass(fqName.shortName(), fqName.parent())

    protected fun getIrClass(fqName: FqName): IrClassSymbol =
        getIrClassOrNull(fqName)
            ?: error("Class \"${fqName.asString()}\" not found! Please make sure that your stdlib version is the same as the compiler.")

    protected fun getIrType(fqName: String): IrType = getIrClass(FqName(fqName)).defaultType
    protected fun getInternalWasmClass(name: String): IrClassSymbol = getIrClass(wasmInternalFqName.child(Name.identifier(name)))

    // Native
    protected fun ClassId.classSymbol() = symbolFinder.findClass(this) ?: error("Class $this is not found")
    protected fun CallableId.propertySymbols() = symbolFinder.findProperties(this).toList()
    protected fun CallableId.functionSymbols() = symbolFinder.findFunctions(this).toList()
    protected fun ClassId.primaryConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.primaryConstructor ?: error("Class ${this} has no primary constructor")).symbol }
    }

    protected fun ClassId.noParametersConstructorSymbol(): Lazy<IrConstructorSymbol> {
        val clazz = classSymbol()
        return lazy { (clazz.owner.constructors.singleOrNull { it.parameters.isEmpty() } ?: error("Class ${this} has no constructor without parameters")).symbol }
    }

    protected fun CallableId.functionSymbol(): IrSimpleFunctionSymbol {
        val elements = functionSymbols()
        require(elements.isNotEmpty()) { "No function $this found" }
        require(elements.size == 1) { "Several functions $this found:\n${elements.joinToString("\n")}" }
        return elements.single()
    }

    protected inline fun CallableId.functionSymbol(crossinline condition: (IrSimpleFunction) -> Boolean): Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = functionSymbols()
        return lazy {
            val elements = unfilteredElements.filter { condition(it.owner) }
            require(elements.isNotEmpty()) { "No function $this found corresponding given condition" }
            require(elements.size == 1) { "Several functions $this found corresponding given condition:\n${elements.joinToString("\n")}" }
            elements.single()
        }
    }

    protected inline fun <K> CallableId.functionSymbolAssociatedBy(crossinline getKey: (IrSimpleFunction) -> K): Lazy<Map<K, IrSimpleFunctionSymbol>> {
        val unfilteredElements = functionSymbols()
        return lazy {
            unfilteredElements.associateBy { getKey(it.owner) }
        }
    }

    protected fun CallableId.getterSymbol() : Lazy<IrSimpleFunctionSymbol> {
        val elements = propertySymbols()
        require(elements.isNotEmpty()) { "No properties $this found" }
        require(elements.size == 1) { "Several properties $this found:\n${elements.joinToString("\n")}" }
        return lazy {
            elements.single().owner.getter!!.symbol
        }
    }

    protected fun CallableId.getterSymbol(extensionReceiverClass: IrClassSymbol?) : Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = propertySymbols()
        require(unfilteredElements.isNotEmpty()) { "No properties $this found" }
        return lazy {
            val elements = unfilteredElements.filter { it.owner.getter?.extensionReceiverClass == extensionReceiverClass }
            require(elements.isNotEmpty()) { "No properties $this found with ${extensionReceiverClass} receiver" }
            require(elements.size == 1) { "Several properties $this found with ${extensionReceiverClass} receiver:\n${elements.joinToString("\n")}" }
            elements.single().owner.getter!!.symbol
        }
    }

    protected val IrFunction.extensionReceiverType get() = parameters.singleOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
    protected val IrFunction.extensionReceiverClass get() = extensionReceiverType?.classOrNull
}

interface FrontendSymbols {
    val asserts: Iterable<IrSimpleFunctionSymbol>
    val arrays: List<IrClassSymbol>

    val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
    val throwUnsupportedOperationException: IrSimpleFunctionSymbol

    val defaultConstructorMarker: IrClassSymbol
    val coroutineContextGetter: IrSimpleFunctionSymbol
    val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
    val coroutineGetContext: IrSimpleFunctionSymbol

    fun isSideEffectFree(call: IrCall): Boolean = false

    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.isTopLevel &&
                        function.getPackageFragment().packageFqName.asString() == "kotlin" &&
                        function.hasShape(extensionReceiver = true) &&
                        function.parameters[0].type.classOrNull?.owner?.fqNameWhenAvailable?.toUnsafe() == StandardNames.FqNames.kProperty0
            }

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean {
            return if (symbol.isBound) {
                symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                    function.isTopLevelInPackage("typeOf", KOTLIN_REFLECT_FQ_NAME) && function.hasShape()
                }
            } else {
                symbol.hasTopLevelEqualFqName(KOTLIN_REFLECT_FQ_NAME.asString(), "typeOf")
            }
        }
    }
}

abstract class FrontendSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendSymbols, BaseSymbolsImpl(irBuiltIns) {
    override val asserts: Iterable<IrSimpleFunctionSymbol> = symbolFinder.findFunctions(Name.identifier("assert"), "kotlin")

    override val arrays: List<IrClassSymbol>
        get() = irBuiltIns.primitiveTypesToPrimitiveArrays.values + irBuiltIns.unsignedTypesToUnsignedArrays.values + irBuiltIns.arrayClass

}

interface FrontendKlibSymbols : FrontendSymbols {
    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    val genericSharedVariableBox: SharedVariableBoxClassInfo
    val primitiveSharedVariableBoxes: Map<IrType, SharedVariableBoxClassInfo>
}

abstract class FrontendKlibSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendKlibSymbols, FrontendSymbolsImpl(irBuiltIns) {
    private fun findSharedVariableBoxClass(suffix: String): FrontendKlibSymbols.SharedVariableBoxClassInfo {
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = symbolFinder.findClass(classId)
            ?: error("Could not find class $classId")
        return FrontendKlibSymbols.SharedVariableBoxClassInfo(boxClass)
    }

    // The SharedVariableBox family of classes exists only in non-JVM stdlib variants, hence the nullability of the properties below.
    override val genericSharedVariableBox: FrontendKlibSymbols.SharedVariableBoxClassInfo = findSharedVariableBoxClass("")
    override val primitiveSharedVariableBoxes: Map<IrType, FrontendKlibSymbols.SharedVariableBoxClassInfo> =
        PrimitiveType.entries.associate {
            irBuiltIns.primitiveTypeToIrType[it]!! to findSharedVariableBoxClass(it.typeName.asString())
        }
}

interface FrontendWebSymbols : FrontendKlibSymbols {
    companion object {
        val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
        val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        val COROUTINE_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "coroutines"))
        val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
    }
}

abstract class FrontendWebSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWebSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {

}

interface FrontendJsSymbols : FrontendWebSymbols {
    val dynamicType: IrDynamicType
        get() = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)

    val jsCode: IrSimpleFunctionSymbol
    val arrayLiteral: IrSimpleFunctionSymbol
    val primitiveToLiteralConstructor: Map<PrimitiveType, IrSimpleFunctionSymbol>
    val arrayConcat: IrSimpleFunctionSymbol
    val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol
    val jsBoxIntrinsic: IrSimpleFunctionSymbol
    val jsUnboxIntrinsic: IrSimpleFunctionSymbol
}

open class FrontendJsSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendJsSymbols, FrontendWebSymbolsImpl(irBuiltIns) {
    override val throwUninitializedPropertyAccessException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "throwUninitializedPropertyAccessException")

    override val throwUnsupportedOperationException =
        symbolFinder.topLevelFunction(kotlinPackageFqn, "throwUnsupportedOperationException")

    override val defaultConstructorMarker =
        symbolFinder.topLevelClass(BASE_JS_PACKAGE, "DefaultConstructorMarker")

    override val coroutineContextGetter =
        symbolFinder.findTopLevelPropertyGetter(FrontendWebSymbols.COROUTINE_PACKAGE_FQNAME, FrontendWebSymbols.COROUTINE_CONTEXT_NAME.asString())
    override val suspendCoroutineUninterceptedOrReturn = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, FrontendWebSymbols.COROUTINE_SUSPEND_OR_RETURN_JS_NAME)
    override val coroutineGetContext = symbolFinder.topLevelFunction(BASE_JS_PACKAGE, FrontendWebSymbols.GET_COROUTINE_CONTEXT_NAME)

    override val jsCode: IrSimpleFunctionSymbol = getInternalJsFunction("js")
    override val arrayLiteral: IrSimpleFunctionSymbol = getInternalJsFunction("arrayLiteral")

    private val primitiveToTypedArrayMap = EnumMap(
        mapOf(
            PrimitiveType.BYTE to "Int8",
            PrimitiveType.SHORT to "Int16",
            PrimitiveType.INT to "Int32",
            PrimitiveType.FLOAT to "Float32",
            PrimitiveType.DOUBLE to "Float64"
        )
    )

    override val primitiveToLiteralConstructor: Map<PrimitiveType, IrSimpleFunctionSymbol> = PrimitiveType.entries.associate { type ->
        type to (primitiveToTypedArrayMap[type]?.let {
            getInternalJsFunction("${it.toLowerCaseAsciiOnly()}ArrayOf")
        } ?: getInternalJsFunction("${type.typeName.asString().toLowerCaseAsciiOnly()}ArrayOf"))
    }

    override val arrayConcat: IrSimpleFunctionSymbol = getInternalJsInRootPackage("arrayConcat")!!
    override val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsOutlinedFunction)
    override val jsBoxIntrinsic: IrSimpleFunctionSymbol = getInternalJsFunction("boxIntrinsic")
    override val jsUnboxIntrinsic: IrSimpleFunctionSymbol = getInternalJsFunction("unboxIntrinsic")

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in primitiveToLiteralConstructor.values ||
                call.symbol == arrayLiteral ||
                call.symbol == arrayConcat ||
                call.symbol == jsBoxIntrinsic ||
                call.symbol == jsUnboxIntrinsic

    companion object {
        private val JsOutlinedFunction = ClassId(BASE_JS_PACKAGE, Name.identifier("JsOutlinedFunction"))
    }
}

interface FrontendWasmSymbols : FrontendWebSymbols {}

open class FrontendWasmSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendWasmSymbols, FrontendWebSymbolsImpl(irBuiltIns) {
    override val throwUninitializedPropertyAccessException =
        getInternalWasmFunction("throwUninitializedPropertyAccessException")

    override val throwUnsupportedOperationException =
        getInternalWasmFunction("throwUnsupportedOperationException")

    override val defaultConstructorMarker =
        getIrClass(FqName("kotlin.wasm.internal.DefaultConstructorMarker"))

    override val coroutineContextGetter =
        symbolFinder.findTopLevelPropertyGetter(FrontendWebSymbols.COROUTINE_PACKAGE_FQNAME, FrontendWebSymbols.COROUTINE_CONTEXT_NAME.asString())
    override val suspendCoroutineUninterceptedOrReturn =
        getInternalWasmFunction("suspendCoroutineUninterceptedOrReturn")
    override val coroutineGetContext =
        getInternalWasmFunction("getCoroutineContext")
}

interface FrontendNativeSymbols : FrontendKlibSymbols {
    val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol
}

open class FrontendNativeSymbolsImpl(irBuiltIns: IrBuiltIns) : FrontendNativeSymbols, FrontendKlibSymbolsImpl(irBuiltIns) {
    private object RuntimeNames {
        val kotlinNativeInternalPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
    }

    private object CallableIds {
        // Internal functions
        private val String.internalCallableId get() = CallableId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
        val throwUninitializedPropertyAccessException = "ThrowUninitializedPropertyAccessException".internalCallableId
        val throwUnsupportedOperationException = "ThrowUnsupportedOperationException".internalCallableId
        val suspendCoroutineUninterceptedOrReturn = "suspendCoroutineUninterceptedOrReturn".internalCallableId
        val getCoroutineContext = "getCoroutineContext".internalCallableId

        // Special stdlib public functions
        val coroutineContext = CallableId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier("coroutineContext"))

        // Built-ins functions
        private val String.builtInsCallableId get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
        val isAssertionArgumentEvaluationEnabled = "isAssertionArgumentEvaluationEnabled".builtInsCallableId
    }

    private object ClassIds {
        // Internal classes
        private val String.internalClassId get() = ClassId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
        val defaultConstructorMarker = "DefaultConstructorMarker".internalClassId
    }

    override val throwUninitializedPropertyAccessException = CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
    override val throwUnsupportedOperationException = CallableIds.throwUnsupportedOperationException.functionSymbol()
    override val defaultConstructorMarker = ClassIds.defaultConstructorMarker.classSymbol()
    override val isAssertionArgumentEvaluationEnabled = CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

    override val coroutineContextGetter by CallableIds.coroutineContext.getterSymbol()
    override val suspendCoroutineUninterceptedOrReturn = CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
    override val coroutineGetContext = CallableIds.getCoroutineContext.functionSymbol()
}
