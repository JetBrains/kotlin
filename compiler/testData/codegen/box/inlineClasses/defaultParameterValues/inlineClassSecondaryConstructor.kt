// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val x: Int) {
    constructor(x: Long = 42L) : this(x.toInt())
}

fun box(): String {
    if (Z().x != 42) throw AssertionError()

    return "OK"
}