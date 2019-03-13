// FIX: Convert to object declaration

open class A {
    open fun test() = "test"
}

interface TestInterface {
    fun inherited(): String
}

class <caret>B {
    companion object : A(), TestInterface {
        override fun test() = "test2"
        override fun inherited() = "inherited"
    }
}
