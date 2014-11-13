// !MARK_DYNAMIC_CALLS

// MODULE[js]: m1
// FILE: k.kt

fun test(d: dynamic) {
    d.foo()
    d.<!DEBUG_INFO_DYNAMIC!>foo<!>(1)

    d.bar()
    d.baz()

    d == 1

    d.equals(1)
    d?.equals(1)

    d.hashCode()
    d?.hashCode()

    d.toString()
    d?.toString()
}

fun Any.foo() {}
fun Any?.bar() {}

fun String.baz() {}

class C {
    fun test(d: dynamic) {
        d.<!DEBUG_INFO_DYNAMIC!>member<!>()
        d.memberExtension()
    }

    fun member() {}
    fun Any.memberExtension() {}
}