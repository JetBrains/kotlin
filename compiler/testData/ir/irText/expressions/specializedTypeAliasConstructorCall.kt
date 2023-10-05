// FIR_IDENTICAL
class Cell<T>(val value: T)

typealias IntAlias = Cell<Int>

fun test() = IntAlias(42)
