// WITH_RUNTIME

@JvmInline
value class R(private val r: Long) {
    fun test() = { ok() }()

    private fun ok() = "OK"
}

fun box() = R(0).test()