// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-81866
// DIAGNOSTICS: -UNCHECKED_CAST
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

import kotlin.coroutines.*

fun test() {
    withFoo {
        this as Foo<Int>
        useIntFoo()
    }

    withBar {
        if (this is Baz) {
            useBaz()
        }
    }
}

@RestrictsSuspension
class Foo<T>

suspend fun Foo<Int>.useIntFoo() { }
fun withFoo(block: suspend Foo<*>.() -> Unit) { }

@RestrictsSuspension
sealed class Bar {
    object Baz : Bar() {
        suspend fun useBaz() { }
    }
}

fun withBar(block: suspend Bar.() -> Unit) { }

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
ifExpression, isExpression, lambdaLiteral, nestedClass, nullableType, objectDeclaration, sealed, smartcast,
starProjection, suspend, thisExpression, typeParameter, typeWithExtension */
