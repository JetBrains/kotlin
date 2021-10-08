// WITH_RUNTIME

inline fun runInline(fn: () -> String) = fn()

@JvmInline
value class R(private val r: Int) {
    fun test() = runInline { "OK" }
}

fun box() = R(0).test()