// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER

package p

fun <T> run(f: () -> T): T {
    return f()
}

private fun foo(a: Int) = run {
    object {
        inner class A
        fun foo() = A()
    }.foo()
}

private fun foo() = run {
    object {
        inner class A
        fun foo() = A()
    }.foo()
}

fun test() {
    var x = foo(1)
    x = <!TYPE_MISMATCH!>foo()<!>
}