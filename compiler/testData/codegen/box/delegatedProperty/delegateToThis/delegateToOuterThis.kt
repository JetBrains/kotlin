// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

class O {
    operator fun getValue(thisRef: Any?, property: Any?) =
        if (thisRef is I) "OK" else "Failed"

    inner class I {
        val s: String by this@O
    }
}

fun box() = O().I().s