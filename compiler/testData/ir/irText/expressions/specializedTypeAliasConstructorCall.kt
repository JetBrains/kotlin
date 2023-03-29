// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

class Cell<T>(val value: T)

typealias IntAlias = Cell<Int>

fun test() = IntAlias(42)
