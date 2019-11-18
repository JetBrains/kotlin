// IGNORE_BACKEND_FIR: JVM_IR
// See also KT-6299
public open class Outer private constructor() {
    class Inner: Outer()
}

fun box(): String {
    val outer = Outer.Inner()
    return "OK"
}