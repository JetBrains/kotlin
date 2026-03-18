/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalSymbolFinderAPI::class)

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.impl.IrDynamicTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.Variance

context(holder: SymbolFinderHolder)
fun findSharedVariableBoxClass(primitiveType: PrimitiveType?): PreSerializationKlibSymbols.SharedVariableBoxClassInfo {
    val suffix = primitiveType?.typeName?.asString() ?: ""
    val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
    val boxClass = classId.classSymbol()
    return PreSerializationKlibSymbols.SharedVariableBoxClassInfo(boxClass)
}

/**
 * This class is a container for symbols that the compiler uses on the backend.
 *
 * ### Hierarchy
 * You can think of it as two separate axis: usage scope and backend kind.
 * 1. By "usage scope" we mean either pre-serialization or backend. We have such separation for several reasons
 *    - **Performance**: it is expensive to load all symbols, but we need only a small part on pre-serialization stage.
 *    - **Availability**: not all symbols are present at the pre-serialization stage.
 *    - **Validation**: symbols that are required for pre-serialization are always present and should be treated with care.
 *      We can't rename or remove them without proper migration.
 * 2. By "backend kind" we mean the target platform: JVM, JS, Wasm, or Native. Some symbols are backend-specific and should appear only in
 *    the corresponding backend.
 *
 * The hierarchy can be represented as follows:
 * ```
 * PreSerializationSymbols.Impl → BackendSymbols → JvmSymbols
 * └ PreSerializationKlibSymbols.Impl → BackendKlibSymbols
 *   ├ PreSerializationWebSymbols.Impl → BackendWebSymbols
 *   │ ├ PreSerializationJsSymbols.Impl → BackendJsSymbols
 *   │ └ PreSerializationWasmSymbols.Impl → BackendWasmSymbols
 *   └ PreSerializationNativeSymbols.Impl → BackendNativeSymbols
 * ```
 *
 *  1. Pre-serialization symbols inheritance is represented from top to bottom. It also follows the general logic around backend
 *     (for example, js and wasm are inherited from web).
 *  2. Backend symbols also follow this pattern, but they also inherit corresponding pre-serialization symbols, so we can avoid duplication.
 *
 *  `JvmSymbols` are special here. They don't have corresponding pre-serialization class because we are not serializing JVM artifacts into
 *  klib.
 *
 *  ### Symbols loading
 *  All symbols loading must be done using extensions on the symbol finder. Usually the process looks as follows
 *  1. During `*Symbols` class construction we call a method on SymbolFinder.
 *     Depending on the implementation, we either get a symbol with the owner immediately (for pre-serialization), or the symbol is put in
 *     the deserialization queue (for backend).
 *  2. During the access of a symbol (later in lowerings), there shouldn't be any unbound symbols.
 *
 *  If we expect to have multiple symbols with the same fully qualified name, then it should be accessed using a lazy filter call.
 *  This guarantees that the symbol will be put in the deserialization queue and properly filtered when first accessed.
 *
 *  Avoid using calls like `functionSymbols().single()`. While this works, it is quite hard to understand that is the problem that something
 *  goes wrong.
 */
interface PreSerializationSymbols {
    val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol
    val throwUnsupportedOperationException: IrSimpleFunctionSymbol

    val syntheticConstructorMarker: IrClassSymbol
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

    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationSymbols, SymbolFinderHolder by irBuiltIns
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
        override val syntheticConstructorMarker: IrClassSymbol = ClassIds.SyntheticConstructorMarker.classSymbol()
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
            CallableIds.throwUninitializedPropertyAccessException.functionSymbol()
        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol =
            CallableIds.throwUnsupportedOperationException.functionSymbol()
    }

    companion object {
        const val THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME = "throwUninitializedPropertyAccessException"
        const val THROW_UNSUPPORTED_OPERATION_NAME = "throwUnsupportedOperationException"
        const val GET_COROUTINE_CONTEXT_NAME = "getCoroutineContext"
        const val COROUTINE_CONTEXT_NAME = "coroutineContext"

        private val kotlinInternalPackageFqn = FqName.fromSegments(listOf("kotlin", "internal"))
        private val String.internalCallableId: CallableId
            get() = CallableId(kotlinInternalPackageFqn, Name.identifier(this))

        private object ClassIds {
            val SyntheticConstructorMarker = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SyntheticConstructorMarker"))
        }

        private object CallableIds {
            val throwUninitializedPropertyAccessException = THROW_UNINITIALIZED_PROPERTY_ACCESS_NAME.internalCallableId
            val throwUnsupportedOperationException = THROW_UNSUPPORTED_OPERATION_NAME.internalCallableId
        }
    }
}

interface PreSerializationWebSymbols : PreSerializationKlibSymbols {
    abstract class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWebSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val coroutineContextGetter: IrSimpleFunctionSymbol by CallableIds.coroutineContextGetter.getterSymbol()

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
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol = CallableIds.coroutineGetContext.functionSymbol()

        override val jsCode by CallableIds.jsCall.functionSymbol() { !it.isExpect }
        override val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = ClassIds.JsOutlinedFunction.classSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_JS_NAME = "suspendCoroutineUninterceptedOrReturnJS"

            private object CallableIds {
                private val String.baseJsCallableId: CallableId
                    get() = CallableId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_JS_NAME.baseJsCallableId
                val coroutineGetContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.baseJsCallableId
                val jsCall: CallableId = "js".baseJsCallableId
            }

            private object ClassIds {
                private val String.baseJsClassId: ClassId
                    get() = ClassId(StandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
                val JsOutlinedFunction: ClassId = "JsOutlinedFunction".baseJsClassId
            }
        }
    }
}

interface PreSerializationWasmSymbols : PreSerializationWebSymbols {
    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationWasmSymbols, PreSerializationWebSymbols.Impl(irBuiltIns) {
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol = CallableIds.coroutineGetContext.functionSymbol()

        companion object {
            private val wasmInternalFqName = FqName.fromSegments(listOf("kotlin", "wasm", "internal"))
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"

            private object CallableIds {
                private val String.internalCallableId: CallableId
                    get() = CallableId(wasmInternalFqName, Name.identifier(this))

                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val coroutineGetContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId
            }
        }
    }
}

interface PreSerializationNativeSymbols : PreSerializationKlibSymbols {
    val asserts: Iterable<IrSimpleFunctionSymbol>
    val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol

    val testInitializer: IrClassSymbol
    val testsProcessed: IrClassSymbol

    val topLevelSuite: IrClassSymbol
    val baseClassSuite: IrClassSymbol
    val testFunctionKind: IrClassSymbol

    val signedIntegerClasses: Set<IrClassSymbol>
    val unsignedIntegerClasses: Set<IrClassSymbol>
    val allIntegerClasses: Set<IrClassSymbol>
    val nativePointed: IrClassSymbol
    val interopCValue: IrClassSymbol
    val interopCPointer: IrClassSymbol
    val interopCValuesRef: IrClassSymbol
    val interopCEnumVar: IrClassSymbol
    val immutableBlobOf: IrSimpleFunctionSymbol
    val createCleaner: IrSimpleFunctionSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val asserts: Iterable<IrSimpleFunctionSymbol> = CallableIds.asserts.functionSymbols()

        override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol =
            CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

        override val testInitializer: IrClassSymbol by ClassIds.testInitializer.lazyClassSymbol(
            LanguageFeature.NativeTestProcessorBeforeSerialization
        )

        override val testsProcessed: IrClassSymbol by ClassIds.testsProcessed.lazyClassSymbol(
            LanguageFeature.NativeTestProcessorBeforeSerialization
        )

        override val topLevelSuite = ClassIds.topLevelSuite.classSymbol()
        override val baseClassSuite = ClassIds.baseClassSuite.classSymbol()
        override val testFunctionKind = ClassIds.testFunctionKind.classSymbol()

        override val signedIntegerClasses = setOf(irBuiltIns.byteClass, irBuiltIns.shortClass, irBuiltIns.intClass, irBuiltIns.longClass)
        override val unsignedIntegerClasses =
            setOf(irBuiltIns.ubyteClass!!, irBuiltIns.ushortClass!!, irBuiltIns.uintClass!!, irBuiltIns.ulongClass!!)
        override val allIntegerClasses = signedIntegerClasses + unsignedIntegerClasses

        override val nativePointed = ClassIds.nativePointed.classSymbol()
        override val interopCValue = ClassIds.interopCValue.classSymbol()
        override val interopCPointer = ClassIds.interopCPointer.classSymbol()
        override val interopCValuesRef = ClassIds.interopCValuesRef.classSymbol()
        override val interopCEnumVar = ClassIds.interopCEnumVar.classSymbol()
        override val immutableBlobOf = CallableIds.immutableBlobOf.functionSymbol()
        override val createCleaner = CallableIds.createCleaner.functionSymbol()

        override val coroutineContextGetter: IrSimpleFunctionSymbol by CallableIds.coroutineContext.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol = CallableIds.getCoroutineContext.functionSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"
            private val kotlinNativeInternalPackageName: FqName = FqName.fromSegments(listOf("kotlin", "native", "internal"))
            private val kotlinNativePackageName: FqName = FqName.fromSegments(listOf("kotlin", "native"))

            private object CallableIds {
                // Internal functions
                private val String.internalCallableId: CallableId
                    get() = CallableId(kotlinNativeInternalPackageName, Name.identifier(this))
                val suspendCoroutineUninterceptedOrReturn: CallableId = COROUTINE_SUSPEND_OR_RETURN_NAME.internalCallableId
                val getCoroutineContext: CallableId = PreSerializationKlibSymbols.GET_COROUTINE_CONTEXT_NAME.internalCallableId

                // Special stdlib public functions
                val coroutineContext: CallableId =
                    CallableId(COROUTINES_PACKAGE_FQ_NAME, Name.identifier(PreSerializationKlibSymbols.COROUTINE_CONTEXT_NAME))

                // Built-ins functions
                private val String.builtInsCallableId: CallableId
                    get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
                val asserts: CallableId = "assert".builtInsCallableId
                val isAssertionArgumentEvaluationEnabled: CallableId = "isAssertionArgumentEvaluationEnabled".builtInsCallableId

                private val String.nativeCallableId get() = CallableId(kotlinNativePackageName, Name.identifier(this))

                val immutableBlobOf = "immutableBlobOf".nativeCallableId

                val createCleaner = CallableId(kotlinNativePackageName.child(Name.identifier("ref")), Name.identifier("createCleaner"))
            }

            private object ClassIds {
                val kotlinNativeInternalTestPackageName = FqName.fromSegments(listOf("kotlin", "native", "internal", "test"))
                private val String.internalTestClassId
                    get() = ClassId(kotlinNativeInternalTestPackageName, Name.identifier(this))
                val testInitializer = "TestInitializer".internalTestClassId
                val testsProcessed = "TestsProcessed".internalTestClassId

                val baseClassSuite = "BaseClassSuite".internalTestClassId
                val topLevelSuite = "TopLevelSuite".internalTestClassId
                val testFunctionKind = "TestFunctionKind".internalTestClassId

                private val String.interopClassId
                    get() = ClassId(NativeStandardInteropNames.cInteropPackage, Name.identifier(this))

                val nativePointed = NativeStandardInteropNames.nativePointed.interopClassId
                val interopCValue = NativeStandardInteropNames.cValue.interopClassId
                val interopCValuesRef = NativeStandardInteropNames.cValuesRef.interopClassId
                val interopCPointer = NativeStandardInteropNames.cPointer.interopClassId
                val interopCEnumVar = NativeStandardInteropNames.cEnumVar.interopClassId
            }
        }
    }
}
