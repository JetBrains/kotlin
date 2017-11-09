import Foo.foo as bar

object Foo {
    fun foo() {}
}

fun test() {
    /*rename*/bar()
}