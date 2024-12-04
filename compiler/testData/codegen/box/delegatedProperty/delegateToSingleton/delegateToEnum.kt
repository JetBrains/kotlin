// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

enum class E {
    OK, NOT_OK
}

operator fun E.getValue(thisRef: Any?, property: Any?): String =
    if (this == E.OK && thisRef == null) "OK" else "Failed"

val s: String by E.OK

fun box(): String = s
