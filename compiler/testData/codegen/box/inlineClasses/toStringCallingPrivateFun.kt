// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class IC(val x: String) {
    private fun privateFun() = x
    override fun toString() = privateFun()
}

fun box(): String {
    val x: Any = IC("OK")
    return x.toString()
}