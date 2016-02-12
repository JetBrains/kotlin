// WITH_RUNTIME
// PARAM_TYPES: A?, kotlin.Any?
// PARAM_TYPES: B, A
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: var a: A? defined in foo
// PARAM_DESCRIPTOR: value-parameter b: B defined in foo
// PARAM_DESCRIPTOR: var c: kotlin.Int defined in foo
// SIBLING:
fun <A: Any, B: A> foo(b: B): Int {
    var a: A? = null
    var c: Int = 1

    <selection>a = b
    c += 2
    println(a)
    println(c)</selection>

    return a.hashCode() ?: 0 + c
}