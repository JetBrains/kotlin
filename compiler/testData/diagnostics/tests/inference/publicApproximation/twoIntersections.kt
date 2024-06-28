// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

interface Base {
    fun base() {}
}
interface Base2 : Base3
interface Base3
interface One : Base, Base2
interface Two : Base, Base2
interface Three : Base, Base3

object O1 : One
object O2 : Two
object O3 : Three

fun <S: Base> intersect(vararg elements: S): S = TODO()
fun <S> intersectNoBound(vararg elements: S): S = TODO()

fun some(a: One, b: Two, c: Three) = intersectNoBound(intersect(a, b), c)

fun test(arg: Base, arg2: Base) {
    some(O1, O2, O3).<!UNRESOLVED_REFERENCE!>base<!>()
}
