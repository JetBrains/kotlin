interface Foo {
    fun foo() {}
}

fun test(): Foo = object : Foo {
    override fun foo() {
        p<caret>rintln()
    }

    fun <caret_target>target() {}
}