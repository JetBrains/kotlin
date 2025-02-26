// ISSUE: KT-67808
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0: error: property must be initialized or be abstract.

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
