// IGNORE_BACKEND_FIR: JVM_IR
// See also KT-6299
public open class Outer private constructor() {
    companion object {
        internal inline fun foo() = Outer()
    }
}

fun box(): String {
    val outer = Outer.foo()
    return "OK"
}