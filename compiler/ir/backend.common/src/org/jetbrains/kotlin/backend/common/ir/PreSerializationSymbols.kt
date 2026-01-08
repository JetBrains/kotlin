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

interface PreSerializationSymbols {
    val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol? // KT-83151 Restore non-nullability of symbols available since 2.3
    val throwUnsupportedOperationException: IrSimpleFunctionSymbol? // KT-83151 Restore non-nullability of symbols available since 2.3

    val syntheticConstructorMarker: IrClassSymbol? // KT-83151 Restore non-nullability of symbols available since 2.3
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

    abstract class Impl(protected val irBuiltIns: IrBuiltIns) : PreSerializationSymbols, SymbolFinderHolder by irBuiltIns {
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
        override val syntheticConstructorMarker: IrClassSymbol? = ClassIds.SyntheticConstructorMarker.classSymbolOrNull()
        override val throwUninitializedPropertyAccessException: IrSimpleFunctionSymbol? =
            CallableIds.throwUninitializedPropertyAccessException.functionSymbolOrNull()
        override val throwUnsupportedOperationException: IrSimpleFunctionSymbol? =
            CallableIds.throwUnsupportedOperationException.functionSymbolOrNull()
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

    val testInitializer: IrClassSymbol? // KT-83151 Restore non-nullability of symbols available since 2.3
    val testsProcessed: IrClassSymbol? // KT-83151 Restore non-nullability of symbols available since 2.3

    val topLevelSuite: IrClassSymbol
    val baseClassSuite: IrClassSymbol
    val testFunctionKind: IrClassSymbol

    open class Impl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbols.Impl(irBuiltIns) {
        override val asserts: Iterable<IrSimpleFunctionSymbol> = CallableIds.asserts.functionSymbols()

        override val isAssertionArgumentEvaluationEnabled: IrSimpleFunctionSymbol =
            CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

        override val testInitializer = ClassIds.testInitializer.classSymbolOrNull()
        override val testsProcessed = ClassIds.testsProcessed.classSymbolOrNull()

        override val topLevelSuite = ClassIds.topLevelSuite.classSymbol()
        override val baseClassSuite = ClassIds.baseClassSuite.classSymbol()
        override val testFunctionKind = ClassIds.testFunctionKind.classSymbol()

        override val coroutineContextGetter: IrSimpleFunctionSymbol by CallableIds.coroutineContext.getterSymbol()
        override val suspendCoroutineUninterceptedOrReturn: IrSimpleFunctionSymbol =
            CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()
        override val coroutineGetContext: IrSimpleFunctionSymbol = CallableIds.getCoroutineContext.functionSymbol()

        companion object {
            private const val COROUTINE_SUSPEND_OR_RETURN_NAME = "suspendCoroutineUninterceptedOrReturn"
            private val kotlinNativeInternalPackageName: FqName = FqName.fromSegments(listOf("kotlin", "native", "internal"))

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
            }
        }
    }
}
