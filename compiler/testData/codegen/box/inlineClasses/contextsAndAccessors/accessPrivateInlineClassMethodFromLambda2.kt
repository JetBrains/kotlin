// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Long) {
    fun test() = { ok() }()

    private fun ok() = "OK"
}

fun box() = R(0).test()