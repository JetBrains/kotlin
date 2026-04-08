// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class O {
    operator fun getValue(thisRef: Any?, property: Any?) =
        if (thisRef is I) "OK" else "Failed"

    inner class I {
        val s: String by this@O
    }
}

fun box() = O().I().s