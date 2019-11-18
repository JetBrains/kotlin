// IGNORE_BACKEND_FIR: JVM_IR
// See also KT-6299
public open class Outer private constructor(val s: String, val f: Boolean = true) {
    class Inner: Outer("xyz")
    class Other: Outer("abc", true)
    class Another: Outer("", false)
}

fun box(): String {
    val outer = Outer.Inner()
    val other = Outer.Other()
    val another = Outer.Another()
    return "OK"
}