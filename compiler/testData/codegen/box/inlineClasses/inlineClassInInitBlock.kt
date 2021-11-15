// WITH_STDLIB

var result = "Fail"

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val value: String) {
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
