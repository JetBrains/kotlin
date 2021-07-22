package d

class T {
    fun baz() = 1
}

fun foo() {
    public val i = 11
    abstract val <!VARIABLE_WITH_NO_TYPE_NO_INITIALIZER!>j<!>
    override fun T.baz() = 2
    private fun bar() = 2
}
