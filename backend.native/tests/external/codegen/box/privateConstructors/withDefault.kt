// See also KT-6299
public open class Outer private constructor(val x: Int = 0) {
    class Inner: Outer()
    class Other: Outer(42)
}

fun box(): String {
    val outer = Outer.Inner()
    val other = Outer.Other()
    return "OK"
}