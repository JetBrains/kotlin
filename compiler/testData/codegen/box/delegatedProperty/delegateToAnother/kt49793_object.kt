// WITH_STDLIB

class Z(val x: String = "OK")

operator fun Z.getValue(x: Any?, y: Any?): Z = this

object O {
    val instance: Z by Z()
    val y by instance::x
}

fun box(): String = O.y
