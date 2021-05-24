// !DIAGNOSTICS: -UNUSED_PARAMETER
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
    smartCastAfterIntersection(one, two)<!UNNECESSARY_SAFE_CALL!>?.<!><!UNRESOLVED_REFERENCE!>base<!>()
}
