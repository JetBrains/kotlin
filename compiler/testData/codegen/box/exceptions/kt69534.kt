// DUMP_IR

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