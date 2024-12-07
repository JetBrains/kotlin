// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL


class C {
    val impl = 123
    val s: String by impl
}

val c = C()

operator fun Any?.getValue(thisRef: Any?, property: Any?) =
    if (this == 123 && thisRef == c) "OK" else "Failed"

fun box() = c.s
