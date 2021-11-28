// WITH_STDLIB

var result = "Fail"

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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
