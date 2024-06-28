class Foo {
    val String.foo : Any
    get() {
        return this@f<caret>oo
    }
}