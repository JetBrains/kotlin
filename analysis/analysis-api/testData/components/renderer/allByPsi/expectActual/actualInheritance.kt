expect abstract class A {
    abstract fun foo()
}

abstract actual class A {
    abstract actual fun foo()
}

class Bar: A() {
    override fun foo() {
        TODO("Not yet implemented")
    }
}