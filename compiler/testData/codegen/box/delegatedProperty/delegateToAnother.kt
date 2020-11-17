// WITH_REFLECT
// WITH_RUNTIME
class C(val x: String)

val x = "O"
val y by ::x
val z by C("K")::x

fun box(): String = y + z
