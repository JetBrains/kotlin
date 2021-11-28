// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class S(val string: String)

enum class Test(val s: S) {
    OK(S("OK"))
}

fun box() = Test.OK.s.string