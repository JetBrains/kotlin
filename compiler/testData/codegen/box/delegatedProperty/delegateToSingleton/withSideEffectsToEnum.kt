// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

var initialized = false

enum class E {
    X;

    companion object {
        init {
            initialized = true
        }
    }
}

operator fun Any?.getValue(x: Any?, y: Any?): String {
    throw RuntimeException()
}

val result by E.X

fun box() = if (initialized) "OK" else "FAILURE"
