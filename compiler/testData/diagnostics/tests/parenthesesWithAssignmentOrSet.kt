// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-70507
// DIAGNOSTICS: -VARIABLE_WITH_REDUNDANT_INITIALIZER
// WITH_STDLIB

class A {
    operator fun plus(x: String): A = this
}

fun Int.foo() = A()

fun foo(a: Array<A>) {
    a[0] = a[0] + ""
    a<!NO_SET_METHOD!>[0]<!> += ""
    (a<!NO_SET_METHOD!>[0]<!>) += ""
    (a[0]) = a[0]

    a[0] = (10 + 1).foo()
}

fun bar() {
    var x = ""

    (x) = ""
    (x) += ""
}

fun baz() {
    (mutableListOf("")) += ""
}

fun bak() {
    val it = mutableListOf(mutableListOf(10))
    (it[0]) += 20
}
