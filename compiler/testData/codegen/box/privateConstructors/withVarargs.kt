// IGNORE_BACKEND: JVM_IR
// See also KT-6299
public open class Outer private constructor(val s: String, vararg i: Int) {
    class Inner: Outer("xyz")
    class Other: Outer("abc", 1, 2, 3)
    class Another: Outer("", 42)
}

fun box(): String {
    val outer = Outer.Inner()
    val other = Outer.Other()
    val another = Outer.Another()
    return "OK"
}