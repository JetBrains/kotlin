open class Cell<T>(val value: T)

typealias CT<T> = Cell<T>
typealias CStr = Cell<String>

class C1 : CT<String>("O")
class C2 : CStr("K")

fun box(): String =
        C1().value + C2().value