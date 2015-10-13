// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER

package p

fun <T> run(f: () -> T): T {
    return f()
}

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
    x = <!TYPE_MISMATCH!>foo()<!>
}