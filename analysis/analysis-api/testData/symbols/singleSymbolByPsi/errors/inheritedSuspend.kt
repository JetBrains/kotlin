// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
open class Foo {
    open suspend fun bar(): String = "Hello, World!"
}

class Bar : Foo() {
    override fun b<caret>ar(): String = "Hello, Kotlin!"
}
