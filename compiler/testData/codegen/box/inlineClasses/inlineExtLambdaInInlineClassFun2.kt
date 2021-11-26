// WITH_STDLIB

inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: String) {
    fun test() = runInlineExt { r }
}

fun box() = R("OK").test()