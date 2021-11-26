// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class R(private val r: Int) {
    fun test() = pf()

    companion object {
        private fun pf() = "OK"
    }
}

fun box() = R(0).test()