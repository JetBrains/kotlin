// WITH_RUNTIME
interface Foo {
    fun foo(): String
}

class Bar : Foo {
    override fun <caret>foo() = java.lang.String.valueOf(42)
}