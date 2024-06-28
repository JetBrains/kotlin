// IGNORE_BACKEND: ANY

// FILE: A.kt
interface Foo {
    fun foo(): String
}

private class FooImpl : Foo {
    private val ok = "OK"
    override fun foo() = ok
}

private inline fun privateMethod() = FooImpl()

internal inline fun internalMethod(): Foo {
    return privateMethod()
}

// FILE: main.kt
fun box(): String {
    return internalMethod().foo()
}
