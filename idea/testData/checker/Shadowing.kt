class A {
    fun component1() = 42
    fun component2() = 42
}

fun foo(a: A, <warning>c</warning>: Int) {
    val (<warning descr="[NAME_SHADOWING] Name shadowed: a"><warning descr="[UNUSED_VARIABLE] Variable 'a' is never used">a</warning></warning>, <warning>b</warning>) = a
    val arr = Array(2) { A() }
    for ((<warning descr="[NAME_SHADOWING] Name shadowed: c">c</warning>, d) in arr) {
    }
}

fun f(<warning>p</warning>: Int): Int {
    val <error>p</error> = 2
    val <error>p</error> = 3
    return p
}