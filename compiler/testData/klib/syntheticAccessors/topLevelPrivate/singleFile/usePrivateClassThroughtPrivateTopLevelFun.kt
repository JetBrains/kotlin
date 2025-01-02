// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

interface Foo {
    fun foo(): String
}

private class FooImpl : Foo {
    private val ok = "OK"
    override fun foo() = ok
}

private inline fun privateMethod() = FooImpl()

@Suppress("IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION")
internal inline fun internalMethod(): Foo {
    return privateMethod()
}

fun box(): String {
    return internalMethod().foo()
}
