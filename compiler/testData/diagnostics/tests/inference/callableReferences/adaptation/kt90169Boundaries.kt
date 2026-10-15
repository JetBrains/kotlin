// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90169
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T> make(f: (T) -> Int): (T) -> Int = f

fun <E> countG(vararg xs: E): Int = xs.size
fun count(vararg xs: Any?): Int = xs.size
fun countS(vararg xs: String): Int = xs.size
fun countCS(vararg xs: CharSequence): Int = xs.size
fun countI(vararg xs: Int): Int = xs.size

class G<E> { fun c(vararg xs: E): Int = xs.size }

fun main(arr: Array<String>, a: Array<Any?>) {
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Any?>, kotlin.Int>")!>::count<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countS<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.CharSequence>, kotlin.Int>")!>::countCS<!>)
    arr.let(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countS<!>)
    a.let(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<(@ParameterName(...) kotlin.Array<(kotlin.Any..kotlin.Any?)>..@ParameterName(...) kotlin.Array<out (kotlin.Any..kotlin.Any?)>?), kotlin.Int>")!>java.util.Objects::hash<!>)
    val f: (Array<String>) -> Int = <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>
    val g: (Array<String>) -> Int = make(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>::countG<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.String>, kotlin.Int>")!>G<String>()::c<!>)
    applyIt(arr, <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.Array<out kotlin.Any?>, kotlin.Int>")!>G<Any?>()::c<!>)
    applyIt(arr) { countG(*it) }
    applyIt(arr) { countG(it) }
    applyIt(intArrayOf(1, 2), <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction1<@ParameterName(...) kotlin.IntArray, kotlin.Int>")!>::countI<!>)

    applyIt("x", <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.String) -> kotlin.Int")!>::countG<!>)
    applyIt(intArrayOf(1, 2), <!DEBUG_INFO_EXPRESSION_TYPE("(kotlin.IntArray) -> kotlin.Int")!>::countG<!>)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, flexibleType, functionDeclaration, functionalType,
integerLiteral, javaCallableReference, lambdaLiteral, localProperty, nullableType, outProjection, propertyDeclaration,
stringLiteral, typeParameter, vararg */
