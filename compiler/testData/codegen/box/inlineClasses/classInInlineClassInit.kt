var result = "Fail"

inline class A(val value: String) {
    init {
        class B {
            init {
                result = value
            }
        }
        B()
    }
}

fun box(): String {
    A("OK")
    return result
}
