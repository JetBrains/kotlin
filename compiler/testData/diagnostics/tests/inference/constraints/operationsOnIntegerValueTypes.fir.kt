// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test() {
    generateException(Data(1), { Data(it.x + 2) })
}

fun <T> generateException(a: T, next: (T) -> T) {}

class Data<out K>(val x: K)