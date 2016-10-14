//KT-2643 Support multi-declarations in Data-Flow analysis
package n

class C {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun test1(c: C) {
    val (a, <!UNUSED_VARIABLE!>b<!>) = c
}

fun test2(c: C) {
    val (a, <!UNUSED_VARIABLE!>b<!>) = c
    a + 3
}

fun test3(c: C) {
    var (a, <!UNUSED_VARIABLE!>b<!>) = c
    <!UNUSED_VALUE!>a =<!> 3
}

fun test4(c: C) {
    var (<!VARIABLE_WITH_REDUNDANT_INITIALIZER!>a<!>, <!UNUSED_VARIABLE!>b<!>) = c
    a = 3
    a + 1
}