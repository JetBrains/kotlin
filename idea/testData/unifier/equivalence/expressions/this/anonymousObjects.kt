interface Foo {
    fun foo()
}

fun foo() {
    val a = <selection>object: Foo {
        override fun foo() {
            this
        }
    }</selection>

    val b = object: Foo {
        override fun foo() {
            this
        }
    }
}