// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER

package p

fun run<T>(f: () -> T): T {
    return f()
}

fun foo(a: Int) = run {
    class A
    A()
}

fun foo() = run {
    class A
    A()
}

fun test() {
    var x = foo(1)
    x = <!TYPE_MISMATCH!>foo()<!>
}