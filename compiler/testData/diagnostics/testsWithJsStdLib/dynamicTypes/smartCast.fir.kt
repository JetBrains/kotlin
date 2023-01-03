// !MARK_DYNAMIC_CALLS

fun foo(d: dynamic) {
    if (d is Foo) {
        d.bar() // resolved statically
        d.baz()
    }
}

class Foo {
    fun bar() {}
}
