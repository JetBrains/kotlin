var result = "Fail"

inline class A(val value: String) {
    fun f() = value + "K"
}

class B(val a: A) {
    val result: String
    init {
        result = a.f()
    }
}

fun box(): String {
    return B(A("O")).result
}
