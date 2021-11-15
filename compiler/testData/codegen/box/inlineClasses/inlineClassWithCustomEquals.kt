// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_STDLIB

@file:Suppress("RESERVED_MEMBER_INSIDE_VALUE_CLASS")

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class Z(val data: Int) {
    override fun equals(other: Any?): Boolean =
        other is Z &&
                data % 256 == other.data % 256
}

fun box(): String {
    if (Z(0) != Z(256)) throw AssertionError()

    return "OK"
}