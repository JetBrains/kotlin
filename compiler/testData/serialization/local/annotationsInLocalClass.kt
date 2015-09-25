// CLASS_NAME_SUFFIX: A$foo$Local

class A {
    annotation class Ann(val info: String)

    fun foo() {
        @Ann("class") class Local {
            @Ann("fun") fun foo(): Local = this
            @field:Ann("val") val x = foo()

            @Ann("inner") inner class Inner
        }
    }
}
