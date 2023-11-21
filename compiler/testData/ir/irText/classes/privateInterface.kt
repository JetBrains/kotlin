// FIR_IDENTICAL
private interface I {
    fun fooString(): String
    fun barString(): String = "bar@I"
    fun bazString(): String = "baz@I"

    fun fooUnit(): Unit
    fun barUnit() {}
    fun bazUnit() {}
}

open class C1 : I {
    override fun fooString() = "foo@C1"
    override fun barString() = "bar@C1"

    override fun fooUnit() {}
    override fun barUnit() {}
}

class C2 : C1()
