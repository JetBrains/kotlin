// IGNORE_BACKEND_K1: JS_IR
// IGNORE_BACKEND_K1: JS_IR_ES6

fun testSimple() = Box<Long>(2L * 3)

inline fun <reified T> testArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

class Box<T>(val value: T)
