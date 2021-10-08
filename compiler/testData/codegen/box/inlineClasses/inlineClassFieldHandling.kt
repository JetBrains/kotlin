// WITH_RUNTIME

var result = "Fail"

@JvmInline
value class A(val value: String)

@JvmInline
value class B(val a: A) {
    init {
        result = a.value
    }
}

fun box(): String {
    B(A("OK"))
    return result
}
