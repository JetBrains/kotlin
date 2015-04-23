var state: String = "Fail"

class A private(x: String = "OK") {
    init {
        state = x
    }

    companion object {
        fun foo() = A()
    }
}

fun box(): String {
    A.foo()
    return state
}
