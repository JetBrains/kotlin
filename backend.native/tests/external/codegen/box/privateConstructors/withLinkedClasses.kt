// See also KT-6299
public open class Outer private constructor(val p: Outer?) {
    object First: Outer(null)
    class Other(p: Outer = First): Outer(p)
}

fun box(): String {
    val second = Outer.Other()
    val third = Outer.Other(second)
    val fourth = Outer.Other(third)
    return "OK"
}