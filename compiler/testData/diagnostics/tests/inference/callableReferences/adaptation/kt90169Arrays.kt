// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90169
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <E> countG(vararg xs: E): Int = xs.size

fun main(arr: Array<String>) {
    applyIt(arrayOf(1, 2), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Int>, kotlin.Int>")!>::countG<!>)
    applyIt(arrayOf<Any?>(1, null), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Any?>, kotlin.Int>")!>::countG<!>)
    applyIt(arr as Array<out String>, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    applyIt(emptyArray<String>(), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    applyIt(arrayOf(arrayOf("a"), arrayOf("b")), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Array<kotlin.String>>, kotlin.Int>")!>::countG<!>)
}

/* GENERATED_FIR_TAGS: asExpression, callableReference, functionDeclaration, functionalType, integerLiteral,
nullableType, outProjection, stringLiteral, typeParameter, vararg */
