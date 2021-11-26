// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Long) {
    private fun ok() = "OK"

    companion object {
        fun test(r: R) = r.ok()
    }
}

fun box() = R.test(R(0))