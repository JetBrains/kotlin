// RUN_PIPELINE_TILL: BACKEND
//KT-2643 Support multi-declarations in Data-Flow analysis
package n

class C {
    operator fun component1() = 1
    operator fun component2() = 2
}

fun test1(c: C) {
    val (a, b) = c
}

fun test2(c: C) {
    val (a, b) = c
    a + 3
}

fun test3(c: C) {
    var (a, b) = c
    a = 3
}

fun test4(c: C) {
    var (a, b) = c
    a = 3
    a + 1
}
