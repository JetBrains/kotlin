// KT-2667 Support multi-declarations in for-loops in control flow analysis
package d

class A {
    operator fun component1() = 1
    operator fun component2() = 2
    operator fun component3() = 3
}

fun foo(list: List<A>) {
    for (var (c1, c2, c3) in list) {
        <!VAL_REASSIGNMENT!>c1<!> = 1
        c3 + 1
    }
}
