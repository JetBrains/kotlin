// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-70507
// DIAGNOSTICS: -VARIABLE_WITH_REDUNDANT_INITIALIZER
// WITH_STDLIB
// LATEST_LV_DIFFERENCE

class A {
    operator fun plus(x: String): A = this
}

fun Int.foo() = A()

fun foo(a: Array<A>) {
    a[0] = a[0] + ""
    a[0] += ""
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(a[0])<!> += ""
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(a[0])<!> = a[0]

    a[0] = (10 + 1).foo()
}

fun bar() {
    var x = ""

    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(x)<!> = ""
    <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(x)<!> += ""
}

fun baz() {
    (mutableListOf("")) += ""
}

fun bak() {
    val it = mutableListOf(mutableListOf(10))
    (it[0]) += 20
}
