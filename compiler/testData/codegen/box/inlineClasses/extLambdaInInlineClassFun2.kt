// WITH_RUNTIME

fun <T> T.runExt(fn: T.() -> String) = fn()

@JvmInline
value class R(private val r: String) {
    fun test() = runExt { r }
}

fun box() = R("OK").test()