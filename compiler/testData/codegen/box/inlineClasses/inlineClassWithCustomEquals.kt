// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

@file:Suppress("RESERVED_MEMBER_INSIDE_INLINE_CLASS")

inline class Z(val data: Int) {
    override fun equals(other: Any?): Boolean =
        other is Z &&
                data % 256 == other.data % 256
}

fun box(): String {
    if (Z(0) != Z(256)) throw AssertionError()

    return "OK"
}