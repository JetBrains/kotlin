open class Foo {
    open suspend fun bar(): String = "Hello, World!"
}

class Bar : Foo() {
    override fun b<caret>ar(): String = "Hello, Kotlin!"
}
