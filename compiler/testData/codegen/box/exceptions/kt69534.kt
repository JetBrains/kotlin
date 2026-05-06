// DUMP_IR
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0
// ^^^ KT-69534 is fixed in 2.1.0-Beta2

sealed class Sas

class Sas2<E : Any>(val error: E): Sas()

fun Sas.foo(): Nothing = when (this) {
    is Sas2<*> -> {
        if (error is Throwable) {
            throw error
        } else {
            TODO()
        }
    }
}

fun box(): String {
    try {
        val sas = Sas2("Error")
        sas.foo()
    } catch (e: Throwable) {
    }
    return "OK"
}
