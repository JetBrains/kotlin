// KT-2667 Support multi-declarations in for-loops in control flow analysis
package d

class A {
    fun component1() = 1
    fun component2() = 2
    fun component3() = 3
}

fun foo(list: List<A>) {
    for (var (c1, c2, c3) in list) {
        <!UNUSED_VALUE!>c1 =<!> 1
        c3 + 1
    }
}
