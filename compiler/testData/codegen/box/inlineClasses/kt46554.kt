var result = "Fail"

inline class A(val value: String) {
    constructor() : this("OK")

    init {
        result = value
    }
}

fun box(): String {
    A()
    return result
}
