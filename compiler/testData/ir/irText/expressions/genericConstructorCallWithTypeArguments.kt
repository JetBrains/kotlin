fun testSimple() = Box<Long>(2 * 3)

inline fun <reified T> testArray(n: Int, crossinline block: () -> T): Array<T> {
    return Array<T>(n) { block() }
}

class Box<T>(val value: T)
