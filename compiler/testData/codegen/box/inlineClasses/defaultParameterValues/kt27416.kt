// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class A(val i: Int) {
    fun foo(s: String = "OK") = s
}

fun box() = A(42).foo()