// !LANGUAGE: +InlineClasses

inline class R(private val r: Long) {
    fun test() = run { ok() }

    private fun ok() = "OK"
}

fun box() = R(0).test()