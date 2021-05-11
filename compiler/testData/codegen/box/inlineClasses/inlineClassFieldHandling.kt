var result = "Fail"

inline class A(val value: String)

inline class B(val a: A) {
    init {
        result = a.value
    }
}

fun box(): String {
    B(A("OK"))
    return result
}
