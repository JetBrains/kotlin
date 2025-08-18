/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
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
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

abstract class BaseSymbolsImpl(protected val irBuiltIns: IrBuiltIns) {
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
        private val String.reflectId: CallableId
            get() = CallableId(KOTLIN_REFLECT_FQ_NAME, Name.identifier(this))
        private val typeOf: CallableId = "typeOf".reflectId

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean {
            return if (symbol.isBound) {
                symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                    function.isTopLevelInPackage(typeOf.callableName.asString(), typeOf.packageName) && function.hasShape()
                }
            } else {
                symbol.hasTopLevelEqualFqName(typeOf.packageName.asString(), typeOf.callableName.asString())
            }
        }
    }

    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationSymbols, BaseSymbolsImpl(irBuiltIns) {
        override val asserts: Iterable<IrSimpleFunctionSymbol> = symbolFinder.findFunctions(CallableIds.asserts.callableName, CallableIds.asserts.packageName)

        override val arrays: List<IrClassSymbol>
            get() = irBuiltIns.primitiveTypesToPrimitiveArrays.values + irBuiltIns.unsignedTypesToUnsignedArrays.values + irBuiltIns.arrayClass

        companion object {
            private object CallableIds {
                private val String.baseId: CallableId
                    get() = CallableId(BASE_KOTLIN_PACKAGE, Name.identifier(this))
                val asserts: CallableId = "assert".baseId
            }
        }
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

    companion object {
        const val THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME = "throwUninitializedPropertyAccessException"
        const val THROW_UNSUPPORTED_OPERATION_NAME = "throwUnsupportedOperationException"
        const val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
        const val COROUTINE_CONTEXT_NAME = "coroutineContext"
        const val DEFAULT_CONSTRUCTOR_MARKET_NAME = "DefaultConstructorMarker"
    }
}

interface PreSerializationWebSymbols : PreSerializationKlibSymbols {
    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWebSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val coroutineContextGetter: IrSimpleFunctionSymbol =
            symbolFinder.findTopLevelPropertyGetter(CallableIds.coroutineContextGetter.packageName, CallableIds.coroutineContextGetter.callableName.asString())

        companion object {
            private object CallableIds {
                val coroutineContextGetter: CallableId =
                    CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(PreSerializationKlibSymbols.COROUTINE_CONTEXT_NAME))
            }
        }
    }
}

interface PreSerializationJsSymbols : PreSerializationWebSymbols {
    val dynamicType: IrDynamicType
        get() = IrDynamicTypeImpl(emptyList(), Variance.INVARIANT)

    val jsCode: IrSimpleFunctionSymbol
    val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationJsSymbols, PreSerializationWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(CallableIds.throwUninitializedPropertyAccessException.packageName, CallableIds.throwUninitializedPropertyAccessException.callableName.asString())

        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(CallableIds.throwUnsupportedOperationException.packageName, CallableIds.throwUnsupportedOperationException.callableName.asString())

        override val defaultConstructorMarker: IrClassSymbol =
            symbolFinder.topLevelClass(ClassIds.defaultConstructorMarker.packageFqName, ClassIds.defaultConstructorMarker.relativeClassName.asString())

        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(CallableIds.suspendCoroutineUninterceptedOrReturn.packageName, CallableIds.suspendCoroutineUninterceptedOrReturn.callableName.asString())
        override val coroutineGetContext: IrSimpleFunctionSymbol =
            symbolFinder.topLevelFunction(CallableIds.coroutineGetContext.packageName, CallableIds.coroutineGetContext.callableName.asString())

        override val jsCode: IrSimpleFunctionSymbol = getInternalJsFunction(CallableIds.jsCall.callableName.asString())
        override val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(ClassIds.JsOutlinedFunction)

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"

            private object CallableIds {
                private val String.rootCallableId: CallableId
                    get() = CallableId(kotlinPackageFqn, Name.identifier(this))
                val throwUninitializedPropertyAccessException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.rootCallableId
                val throwUnsupportedOperationException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.rootCallableId

                private val String.baseJsCallableId: CallableId
                    get() = CallableId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_JS_NAME.baseJsCallableId
                val coroutineGetContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.baseJsCallableId
                val jsCall: CallableId = "js".baseJsCallableId
            }

            private object ClassIds {
                private val String.baseJsClassId: ClassId
                    get() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val defaultConstructorMarker: ClassId = PreSerializationKlibSymbols.DEFAULT_CONSTRUCTOR_MARKET_NAME.baseJsClassId
                val JsOutlinedFunction: ClassId = "JsOutlinedFunction".baseJsClassId
            }
        }
    }
}

interface PreSerializationWasmSymbols : PreSerializationWebSymbols {
    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWasmSymbols, PreSerializationWebSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            getInternalWasmFunction(CallableIds.throwUninitializedPropertyAccessException.callableName.asString())

        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            getInternalWasmFunction(CallableIds.throwUnsupportedOperationException.callableName.asString())

        override val defaultConstructorMarker: IrClassSymbol =
            getIrClass(FqName("kotlin.wasm.internal.${ClassIds.defaultConstructorMarker.relativeClassName.asString()}"))

        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            getInternalWasmFunction(CallableIds.suspendCoroutineUninterceptedOrReturn.callableName.asString())
        override val coroutineGetContext: IrSimpleFunctionSymbol =
            getInternalWasmFunction(CallableIds.coroutineGetContext.callableName.asString())

        companion object {
            val wasmInternalFqName = FqName.fromSegments(listOf("kotlin", "wasm", "internal"))
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"

            private object CallableIds {
                private val String.internalCallableId: CallableId
                    get() = CallableId(wasmInternalFqName, Name.identifier(this))
                val throwUninitializedPropertyAccessException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.internalCallableId
                val throwUnsupportedOperationException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.internalCallableId

                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val coroutineGetContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId
            }

            private object ClassIds {
                private val String.internalClassId: ClassId
                    get() = ClassId(wasmInternalFqName, Name.identifier(this))
                val defaultConstructorMarker: ClassId = PreSerializationKlibSymbols.DEFAULT_CONSTRUCTOR_MARKET_NAME.internalClassId
            }
        }
    }
}

interface PreSerializationNativeSymbols : PreSerializationKlibSymbols {
    val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            CallableIds.throwUnsupportedOperationException.functionSymbol()
        override val defaultConstructorMarker: IrClassSymbol = ClassIds.defaultConstructorMarker.classSymbol()
        override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol =
            CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

        override val coroutineContextGetter: IrSimpleFunctionSymbol by CallableIds.coroutineContext.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol =
            CallableIds.getCoroutineContext.functionSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"
            private val kotlinNativeInternalPackageName: FqName = FqName.fromSegments(listOf("kotlin", "native", "internal"))

            private object CallableIds {
                // Internal functions
                private val String.internalCallableId: CallableId
                    get() = CallableId(kotlinNativeInternalPackageName, Name.identifier(this))
                val throwUninitializedPropertyAccessException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.capitalizeAsciiOnly().internalCallableId
                val throwUnsupportedOperationException: CallableId =
                    PreSerializationKlibSymbols.THROW_UNSUPPORTED_OPERATION_NAME.capitalizeAsciiOnly().internalCallableId
                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val getCoroutineContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId

                // Special stdlib public functions
                val coroutineContext: CallableId =
                    CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(PreSerializationKlibSymbols.COROUTINE_CONTEXT_NAME))

                // Built-ins functions
                private val String.builtInsCallableId: CallableId
                    get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
                val isAssertionArgumentEvaluationEnabled: CallableId = "isAssertionArgumentEvaluationEnabled".builtInsCallableId
            }

            private object ClassIds {
                // Internal classes
                private val String.internalClassId: ClassId
                    get() = ClassId(kotlinNativeInternalPackageName, Name.identifier(this))
                val defaultConstructorMarker: ClassId =
                    PreSerializationKlibSymbols.DEFAULT_CONSTRUCTOR_MARKET_NAME.internalClassId
            }
        }
    }
}
