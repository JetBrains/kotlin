// WITH_STDLIB

var result = "Fail"

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val value: String) {
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
