// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

var initialized = false

object O {
    init {
        initialized = true
    }

    operator fun getValue(x: Any?, y: Any?): String {
        throw RuntimeException()
    }
}

class C {
    val s: String by O
}

fun box(): String {
    val c = C()
    return if (initialized) "OK" else "FAILURE"
}
