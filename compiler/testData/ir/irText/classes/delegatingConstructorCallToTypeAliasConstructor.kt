// MUTE_SIGNATURE_COMPARISON_K2: JVM_IR
// ^ KT-57429

open class Cell<T>(val value: T)

typealias CT<T> = Cell<T>
typealias CStr = Cell<String>

class C1 : CT<String>("O")
class C2 : CStr("K")
