// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

class C {
    val s: String by this
}

val c = C()

operator fun C.getValue(thisRef: Any?, property: Any?) =
    if (this == c && thisRef == c) "OK" else "Failed"

fun box() = c.s