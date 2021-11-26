// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class UInt(private val data: Int) {
    fun result(): String = if (data == 1) "OK" else "fail"
}

fun f(): UInt {
    val unull = UInt(1) ?: null
    return nonNull(unull)
}

fun nonNull(u: UInt?) = u!!

fun box(): String {
    return f().result()
}