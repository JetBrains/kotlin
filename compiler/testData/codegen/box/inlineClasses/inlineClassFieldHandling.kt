// WITH_STDLIB

var result = "Fail"

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val value: String)

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class B(val a: A) {
    init {
        result = a.value
    }
}

fun box(): String {
    B(A("OK"))
    return result
}
