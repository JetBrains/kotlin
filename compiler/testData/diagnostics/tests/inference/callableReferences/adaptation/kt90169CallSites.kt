// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90169
// WITH_STDLIB

// FILE: JFunction.java
public interface JFunction<T, R> {
    R apply(T t);
}

// FILE: main.kt
fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T> applyRev(f: (T) -> Int, t: T): Int = f(t)
fun <T : Any> applyNN(t: T, f: (T) -> Int): Int = f(t)
fun <T> applyJ(t: T, f: JFunction<T, Int>): Int = f.apply(t)

fun interface F<T> { fun f(t: T): Int }
fun <T> applyF(t: T, f: F<T>): Int = f.f(t)

fun <E> countG(vararg xs: E): Int = xs.size

fun main(arr: Array<String>, n: Array<String>?) {
    applyIt<Array<String>>(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    arr.let(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    n?.let(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    with(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.Int>")!>listOf(arr).map(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)<!>
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.collections.List<kotlin.String>")!>listOf(arr).flatMap(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.collections.List<kotlin.String>>")!>::listOf<!>)<!>

    applyRev(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>, arr)
    applyNN(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    applyJ(arr, <!DEBUG_INFO_EXPRESSION_TYPE("((kotlin.Array<kotlin.String>..kotlin.Array<kotlin.String>?)) -> kotlin.Int")!>::countG<!>)
    applyF(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
}

/* GENERATED_FIR_TAGS: callableReference, flexibleType, funInterface, functionDeclaration, functionalType,
interfaceDeclaration, javaType, nullableType, outProjection, safeCall, samConversion, typeConstraint, typeParameter,
vararg */
