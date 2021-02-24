// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

interface Base {
    fun base() {}
}
interface Base2
interface One : Base, Base2
interface Two : Base, Base2

fun <S: Base> intersectNullable(vararg elements: S): S? = TODO()

fun smartCastAfterIntersection(a: One, b: Two) = run {
    val v = intersectNullable(a, b)
    if (v == null) throw Exception()
    v
}

fun test(one: One, two: Two) {
    smartCastAfterIntersection(one, two)?.<!UNRESOLVED_REFERENCE!>base<!>()
}
