// WITH_RUNTIME

var result = "Fail"

@JvmInline
value class A(val value: String) {
    constructor() : this("OK")

    init {
        result = value
    }
}

fun box(): String {
    A()
    return result
}
