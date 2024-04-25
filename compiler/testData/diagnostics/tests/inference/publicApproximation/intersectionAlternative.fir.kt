// DIAGNOSTICS: -UNUSED_PARAMETER

interface Bound
interface Bound1 : Bound
interface Bound2 : Bound
interface Bound3
object First : Bound1, Bound2, Bound3
object Second : Bound1, Bound2, Bound3

fun <S> intersect(vararg elements: S): S where S : Bound1, S : Bound2 = TODO()

fun testIntersectionAlternative() = intersect(First, Second)

fun test() {
    testIntersectionAlternative()
}
