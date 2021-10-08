// WITH_RUNTIME

fun <T> T.runExt(fn: T.() -> String) = fn()

@JvmInline
value class R(private val r: Int) {
    fun test() = runExt { "OK" }
}

fun box() = R(0).test()