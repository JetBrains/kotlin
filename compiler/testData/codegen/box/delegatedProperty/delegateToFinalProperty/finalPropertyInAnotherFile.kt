// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

//FILE file1.kt
val impl = 123

//FILE file2.kt
operator fun Any?.getValue(thisRef: Any?, property: Any?) =
    if (this == 123 && thisRef == null) "OK" else "Failed"

val s: String by impl

fun box() = s