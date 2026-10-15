// RUN_PIPELINE_TILL: FRONTEND
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
    val s: String = arr.<!INITIALIZER_TYPE_MISMATCH!>let<!>(::firstG)
    val l: List<String> = arr.<!INITIALIZER_TYPE_MISMATCH!>let<!>(::listOf)
    arr.let(::listOf).first().<!UNRESOLVED_REFERENCE!>length<!>
    arr.let<Array<String>, List<String>>(::<!NONE_APPLICABLE!>listOf<!>)
    applyR<Array<String>, String>(arr, ::<!INAPPLICABLE_CANDIDATE!>firstG<!>)

    applyIt(arr, ::<!INAPPLICABLE_CANDIDATE!>countCS<!>)
    applyIt(arrayOf(listOf(1), listOf(2)), ::<!INAPPLICABLE_CANDIDATE!>countL<!>)
    applyK(arr, <!ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE!>::countG<!>)
}

/* GENERATED_FIR_TAGS: callableReference, capturedType, functionDeclaration, functionalType, integerLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, typeConstraint, typeParameter, vararg */
