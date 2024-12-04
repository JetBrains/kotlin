// ISSUE: KT-67808

open class Base<T> {
    val x: Any?

    init {
        this as Derived
        x = "OK"
    }
}

class Derived: Base<String>()

fun box(): String {
    val d = Derived()
    return d.x as String
}
