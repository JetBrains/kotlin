import A.Companion.run

interface Action<T> {
    fun run(t: T)
}

open class A {
    companion object : Action<String> {
        override fun run(t: String) {
        }
    }
}

class B : A() {
    fun foo() {
        run("")
    }
}