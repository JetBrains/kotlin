// WITH_RUNTIME

fun <T> eval(fn: () -> T) = fn()

@JvmInline
value class R(private val r: Int) {
    fun test() = eval { ok() }

    private fun ok() = "OK"
}

fun box() = R(0).test()