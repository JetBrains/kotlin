// WITH_STDLIB

inline fun runInline(fn: () -> String) = fn()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Int) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()