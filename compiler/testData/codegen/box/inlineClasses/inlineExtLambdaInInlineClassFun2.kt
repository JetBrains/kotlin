// WITH_RUNTIME

inline fun <T> T.runInlineExt(fn: T.() -> String) = fn()

@JvmInline
value class R(private val r: String) {
    fun test() = runInlineExt { r }
}

fun box() = R("OK").test()