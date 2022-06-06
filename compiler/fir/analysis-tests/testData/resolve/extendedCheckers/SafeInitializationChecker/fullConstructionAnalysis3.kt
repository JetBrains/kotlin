open class E {
    open val x = "Hello"
    open fun foo(bool: Boolean) = x
}

interface I {
    fun foo(bool: Boolean) = "Hello"
}

class F : E(), I {
    val y = foo(true)
    <!ACCESS_TO_UNINITIALIZED_VALUE,ACCESS_TO_UNINITIALIZED_VALUE!>override val x = foo(false)<!>

    override fun foo(bool: Boolean): String {
        return if (bool) super<I>.foo(bool).substring(1) else super<E>.foo(bool).substring(1)
    }
}
