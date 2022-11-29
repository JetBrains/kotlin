// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

operator fun Any?.getValue(thisRef: Any?, property: Any?) =
    if (this == null && thisRef == null) "OK" else "Failed"

val s: String by null

fun box() = s
