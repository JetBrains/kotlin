/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-220
 * PRIMARY LINKS: expressions, call-and-property-access-expressions, callable-references -> paragraph 3 -> sentence 1
 */
fun f1() = Map::hashCode
fun f2() = Map.Entry::hashCode

class Outer<T> {
    inner class Inner
}

fun f3() = Outer.Inner::hashCode
