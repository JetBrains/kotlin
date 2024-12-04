// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

enum class E {
    OK, NOT_OK
}

class C {
    val s: String by E.OK
}

val c = C()

operator fun E.getValue(thisRef: Any?, property: Any?): String =
    if (this == E.OK && thisRef == c) "OK" else "Failed"

fun box(): String = c.s
