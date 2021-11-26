// WITH_STDLIB

fun <T> T.runExt(fn: T.() -> String) = fn()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: String) {
    fun test() = runExt { r }
}

fun box() = R("OK").test()