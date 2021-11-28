// WITH_STDLIB

fun <T> eval(fn: () -> T) = fn()

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Int) {
    fun test() = eval { ok() }

    private fun ok() = "OK"
}

fun box() = R(0).test()