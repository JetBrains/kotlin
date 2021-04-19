// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER

package p

private fun foo(a: Int) = run {
    class A
    A()
}

private fun foo() = run {
    class A
    A()
}

fun test() {
    var x = foo(1)
    x = <!ASSIGNMENT_TYPE_MISMATCH!>foo()<!>
}
