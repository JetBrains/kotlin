class A {
    operator fun component1() = 42
    operator fun component2() = 42
}

fun arrayA(): Array<A> = null!!

fun foo(a: A, <warning descr="[UNUSED_PARAMETER] Parameter 'c' is never used">c</warning>: Int) {
    val (<warning descr="[NAME_SHADOWING] Name shadowed: a"><warning descr="[UNUSED_VARIABLE] Variable 'a' is never used">a</warning></warning>, <warning descr="[UNUSED_VARIABLE] Variable 'b' is never used">b</warning>) = a
    val arr = arrayA()
    for ((<warning descr="[NAME_SHADOWING] Name shadowed: c"><warning descr="[UNUSED_VARIABLE] Variable 'c' is never used">c</warning></warning>, <warning descr="[UNUSED_VARIABLE] Variable 'd' is never used">d</warning>) in arr) {
    }
}

fun f(<warning descr="[UNUSED_PARAMETER] Parameter 'p' is never used">p</warning>: Int): Int {
    val <error descr="">p</error> = 2
    val <error descr="">p</error> = 3
    return p
}
