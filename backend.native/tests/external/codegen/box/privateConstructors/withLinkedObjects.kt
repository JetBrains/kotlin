// See also KT-6299
public open class Outer private constructor(val p: Outer?) {
    object Inner: Outer(null)
    object Other: Outer(Inner)
    object Another: Outer(Other)
}

fun box(): String {
    val outer = Outer.Inner
    val other = Outer.Other
    val another = Outer.Another
    return "OK"
}