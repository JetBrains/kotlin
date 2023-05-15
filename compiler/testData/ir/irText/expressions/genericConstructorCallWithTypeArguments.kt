// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

fun testSimple() = Box<Long>(2L * 3)

inline fun <reified T> testArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

class Box<T>(val value: T)
