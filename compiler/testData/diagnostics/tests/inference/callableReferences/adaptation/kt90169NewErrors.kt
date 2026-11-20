// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-90169
// WITH_STDLIB

fun <T> applyIt(t: T, f: (T) -> Int): Int = f(t)
fun <T, R> applyR(t: T, f: (T) -> R): R = f(t)
fun <T> applyK(t: T, f: kotlin.reflect.KFunction1<T, Int>): Int = f(t)

fun <E> countG(vararg xs: E): Int = xs.size
fun <E> firstG(vararg xs: E): E = xs[0]
fun <E : CharSequence> countCS(vararg xs: E): Int = xs.size
fun <E> countL(vararg xs: List<E>): Int = xs.size

fun main(arr: Array<String>) {
    val s: String = arr.let(::firstG)
    val l: List<String> = arr.let(::listOf)
    arr.let(::listOf).first().length
    arr.let<Array<String>, List<String>>(::listOf)
    applyR<Array<String>, String>(arr, ::firstG)

    applyIt(arr, ::countCS)
    applyIt(arrayOf(listOf(1), listOf(2)), ::countL)
    applyK(arr, ::countG)
}

/* GENERATED_FIR_TAGS: callableReference, capturedType, functionDeclaration, functionalType, integerLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, typeConstraint, typeParameter, vararg */
