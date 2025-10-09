val x = X("first", "second", "third")

class Y(a: String, b: String, c: String): X(a, b, c) {
    val x: String = "fourth"
    val y: String = "fifth"
    override val z: String = "sixth"
    val superz: String = super.z
}

val y = Y("seventh", "eighth", "ninth")

fun lib(): String {
    return when {
        x.w != "firstsecond" -> "fail 1"
        x.z != "third" -> "fail 2"
        y.x != "fourth" -> "fail 3"
        y.y != "fifth" -> "fail 4"
        y.z != "sixth" -> "fail 5"
        y.superz != "ninth" -> "fail 6"

        else -> "OK"
    }
}

