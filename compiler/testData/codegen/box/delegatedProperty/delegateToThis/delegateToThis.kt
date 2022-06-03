// WITH_STDLIB
// CHECK_BYTECODE_LISTING

class C {
    operator fun getValue(thisRef: Any?, property: Any?) =
        if (thisRef == this) "OK" else "Failed"

    val s: String by this
}

fun box() = C().s