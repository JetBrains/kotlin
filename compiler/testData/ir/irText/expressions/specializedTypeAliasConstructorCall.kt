// IGNORE_BACKEND_FIR: ANY
// See KT-46996

class Cell<T>(val value: T)

typealias IntAlias = Cell<Int>

fun test() = IntAlias(42)
