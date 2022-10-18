// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

object O {
    val impl = 123
}

operator fun Any?.getValue(thisRef: Any?, property: Any?) =
    if (this == 123 && thisRef == null) "OK" else "Failed"

val s: String by O.impl

fun box() = s
