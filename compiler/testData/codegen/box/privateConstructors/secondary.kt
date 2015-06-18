// See also KT-6299
public open class Outer private constructor(val x: Int) {
    constructor(): this(42)
}

fun box(): String {
    val outer = Outer()
    return "OK"
}