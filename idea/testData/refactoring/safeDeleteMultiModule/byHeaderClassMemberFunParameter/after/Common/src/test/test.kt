package test

expect class Foo {
    fun foo()
}

fun test(f: Foo) {
    f.foo()
    f.foo()
}