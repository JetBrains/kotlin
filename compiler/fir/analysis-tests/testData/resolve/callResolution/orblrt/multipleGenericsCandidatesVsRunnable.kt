// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK
// WITH_STDLIB

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

fun foo(runnable: Runnable): Runnable = TODO()

fun <T1> foo(s: Supplier<T1>): Supplier<T1> = TODO()

fun <T2> foo(c: Consumer<T2>): Consumer<T2> = TODO()

fun <T3, U3> foo(f: Function<T3, U3>): Function<T3, U3> = TODO()

fun <T4> foo(action: () -> T4): () -> T4 = TODO()

fun expectsUnitFunctionType(b: () -> Unit) {}

fun myUnit() {}

fun main() {
    val x1 = foo { "" }
    val x2 = foo { myUnit() }
    val x3 = foo { x: String -> "" }
    val x4 = foo { x: String -> myUnit() }
    val x5 = CompletableFuture.supplyAsync(foo { "" })

    val b: () -> Unit = {}

    expectsUnitFunctionType(<!ARGUMENT_TYPE_MISMATCH!>foo(b)<!>)

    <!DEBUG_INFO_EXPRESSION_TYPE("() -> kotlin.String")!>x1<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("() -> kotlin.Unit")!>x2<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("java.util.function.Function<kotlin.String, kotlin.String>")!>x3<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("java.util.function.Consumer<kotlin.String>")!>x4<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("(java.util.concurrent.CompletableFuture<(kotlin.String..kotlin.String?)>..java.util.concurrent.CompletableFuture<(kotlin.String..kotlin.String?)>?)")!>x5<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, samConversion, stringLiteral, typeParameter */
