interface Foo {
    fun foo() {}
}

fun test(): Foo = object : Foo {
    override fun foo() {
        p<caret>rintln()
    }

    protected fun <caret_target>target() {}
}