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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
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

abstract class BaseSymbolsImpl(val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder

    // TODO KT-79436 unify backend specific functions and remove the old ones
    protected fun findSharedVariableBoxClass(primitiveType: PrimitiveType?): PreSerializationKlibSymbols.SharedVariableBoxClassInfo {
        val suffix = primitiveType?.typeName?.asString() ?: ""
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = symbolFinder.findClass(classId)
            ?: error("Could not find class $classId")
        return PreSerializationKlibSymbols.SharedVariableBoxClassInfo(boxClass)
    }

    // JS
    companion object {
        val BASE_JS_PACKAGE = BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))
    }

    protected fun getInternalJsFunction(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), BASE_JS_PACKAGE).single()

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

    protected fun getEnumsFunction(name: String): IrSimpleFunctionSymbol = getFunction(name, enumsInternalPackageFqName)

    protected fun getIrClassOrNull(fqName: FqName): IrClassSymbol? = symbolFinder.findClass(fqName.shortName(), fqName.parent())

    protected fun getIrClass(fqName: FqName): IrClassSymbol =
        getIrClassOrNull(fqName)
            ?: error("Class \"${fqName.asString()}\" not found! Please make sure that your stdlib version is the same as the compiler.")

    protected fun getIrType(fqName: String): IrType = getIrClass(FqName(fqName)).defaultType
    protected fun getInternalWasmClass(name: String): IrClassSymbol = getIrClass(wasmInternalFqName.child(Name.identifier(name)))

    // Native
    protected fun ClassId.classSymbol(): IrClassSymbol = symbolFinder.findClass(this) ?: error("Class $this is not found")
    protected fun CallableId.propertySymbols(): List<IrPropertySymbol> = symbolFinder.findProperties(this).toList()
    protected fun CallableId.functionSymbols(): List<IrSimpleFunctionSymbol> = symbolFinder.findFunctions(this).toList()
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

    protected fun CallableId.getterSymbol(): Lazy<IrSimpleFunctionSymbol> {
        val elements = propertySymbols()
        require(elements.isNotEmpty()) { "No properties $this found" }
        require(elements.size == 1) { "Several properties $this found:\n${elements.joinToString("\n")}" }
        return lazy {
            elements.single().owner.getter!!.symbol
        }
    }

    protected fun CallableId.getterSymbol(extensionReceiverClass: IrClassSymbol?): Lazy<IrSimpleFunctionSymbol> {
        val unfilteredElements = propertySymbols()
        require(unfilteredElements.isNotEmpty()) { "No properties $this found" }
        return lazy {
            val elements = unfilteredElements.filter { it.owner.getter?.extensionReceiverClass == extensionReceiverClass }
            require(elements.isNotEmpty()) { "No properties $this found with ${extensionReceiverClass} receiver" }
            require(elements.size == 1) { "Several properties $this found with ${extensionReceiverClass} receiver:\n${elements.joinToString("\n")}" }
            elements.single().owner.getter!!.symbol
        }
    }

    protected val IrFunction.extensionReceiverType: IrType? get() = parameters.singleOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
    protected val IrFunction.extensionReceiverClass: IrClassSymbol? get() = extensionReceiverType?.classOrNull
}

interface PreSerializationSymbols {
    val asserts: Iterable<IrSimpleFunctionSymbol>
    val arrays: List<IrClassSymbol>

    val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
    val throwUnsupportedOperationException: IrSimpleFunctionSymbol

    val defaultConstructorMarker: IrClassSymbol
    val coroutineContextGetter: IrSimpleFunctionSymbol
    val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol
    val coroutineGetContext: IrSimpleFunctionSymbol

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

    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationSymbols, BaseSymbolsImpl(irBuiltIns) {
        override val asserts: Iterable<IrSimpleFunctionSymbol> = symbolFinder.findFunctions(Name.identifier("assert"), "kotlin")

        override val arrays: List<IrClassSymbol>
            get() = irBuiltIns.primitiveTypesToPrimitiveArrays.values + irBuiltIns.unsignedTypesToUnsignedArrays.values + irBuiltIns.arrayClass

    }
}

interface PreSerializationKlibSymbols : PreSerializationSymbols {
    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    val genericSharedVariableBox: SharedVariableBoxClassInfo

    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationKlibSymbols, PreSerializationSymbols.Impl(irBuiltIns) {
        override val genericSharedVariableBox: SharedVariableBoxClassInfo = findSharedVariableBoxClass(null)
    }
}

interface PreSerializationWebSymbols : PreSerializationKlibSymbols {
    companion object {
        val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
        val COROUTINE_CONTEXT_NAME = Name.identifier("coroutineContext")
        val COROUTINE_PACKAGE_FQNAME = FqName.fromSegments(listOf("kotlin", "coroutines"))
        val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"
    }

    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWebSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val coroutineContextGetter =
            symbolFinder.findTopLevelPropertyGetter(COROUTINE_PACKAGE_FQNAME, COROUTINE_CONTEXT_NAME.asString())
    }
}

interface PreSerializationJsSymbols : PreSerializationWebSymbols {
    val dynamicType: IrDynamicType
        get() = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)

    val jsCode: IrSimpleFunctionSymbol
    val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationJsSymbols, PreSerializationWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(kotlinPackageFqn, "throwUninitializedPropertyAccessException")

        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(kotlinPackageFqn, "throwUnsupportedOperationException")

        override val defaultConstructorMarker: IrClassSymbol =
            symbolFinder.topLevelClass(BASE_JS_PACKAGE, "DefaultConstructorMarker")

        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(BASE_JS_PACKAGE, PreSerializationWebSymbols.COROUTINE_SUSPEND_OR_RETURN_JS_NAME)
        override val coroutineGetContext: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(BASE_JS_PACKAGE, PreSerializationWebSymbols.GET_COROUTINE_CONTEXT_NAME)

        override val jsCode: IrSimpleFunctionSymbol = getInternalJsFunction("js")
        override val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsOutlinedFunction)

        companion object {
            private val JsOutlinedFunction: ClassId =
                ClassId(BASE_JS_PACKAGE, Name.identifier("JsOutlinedFunction"))
        }
    }
}

interface PreSerializationWasmSymbols : PreSerializationWebSymbols {
    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWasmSymbols, PreSerializationWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            getInternalWasmFunction("throwUninitializedPropertyAccessException")

        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            getInternalWasmFunction("throwUnsupportedOperationException")

        override val defaultConstructorMarker: IrClassSymbol =
            getIrClass(FqName("kotlin.wasm.internal.DefaultConstructorMarker"))

        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            getInternalWasmFunction("suspendCoroutineUninterceptedOrReturn")

        override val coroutineGetContext: IrSimpleFunctionSymbol =
            getInternalWasmFunction("getCoroutineContext")
    }
}

interface PreSerializationNativeSymbols : PreSerializationKlibSymbols {
    val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        private object RuntimeNames {
            val kotlinNativeInternalPackageName: FqName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
        }

        private object CallableIds {
            // Internal functions
            private val String.internalCallableId: CallableId
                get() = CallableId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
            val throwUninitializedPropertyAccessException: CallableId = "ThrowUninitializedPropertyAccessException".internalCallableId
            val throwUnsupportedOperationException: CallableId = "ThrowUnsupportedOperationException".internalCallableId
            val suspendCoroutineUninterceptedOrReturn: CallableId = "suspendCoroutineUninterceptedOrReturn".internalCallableId
            val getCoroutineContext: CallableId = "getCoroutineContext".internalCallableId

            // Special stdlib public functions
            val coroutineContext: CallableId =
                CallableId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier("coroutineContext"))

            // Built-ins functions
            private val String.builtInsCallableId: CallableId
                get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
            val isAssertionArgumentEvaluationEnabled: CallableId = "isAssertionArgumentEvaluationEnabled".builtInsCallableId
        }

        private object ClassIds {
            // Internal classes
            private val String.internalClassId: ClassId
                get() = ClassId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
            val defaultConstructorMarker: ClassId = "DefaultConstructorMarker".internalClassId
        }

        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            CallableIds.throwUnsupportedOperationException.functionSymbol()
        override val defaultConstructorMarker: IrClassSymbol =
            ClassIds.defaultConstructorMarker.classSymbol()
        override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol =
            CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

        override val coroutineContextGetter: IrSimpleFunctionSymbol by CallableIds.coroutineContext.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol =
            CallableIds.getCoroutineContext.functionSymbol()
    }
}
