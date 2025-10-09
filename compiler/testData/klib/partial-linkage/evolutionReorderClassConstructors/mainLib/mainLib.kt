open class Y: X("nineth") 
open class Z: X("seventh", "eighth") 
open class W: X("first", "second", "third")

fun lib(): String {
    val y = Y()
    val z = Z()
    val w = W()
    return when {
        y.y != "nineth" -> "fail 1"
        z.y != "tenth" -> "fail 2"
        w.y != "second" -> "fail 3"

        else -> "OK"
    }
}

