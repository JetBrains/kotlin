class MyClass {
    fun foo() = 1
}

MyClass().foo()

interface I {
    fun foo(): Int
}

val i = object: I {
    override fun foo(): Int = 1
}
i.foo()
