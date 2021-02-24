// IS_APPLICABLE: false

fun foo(z: Int, y: Int = 0) = y + z

val x = { arg: Int <caret>-> foo(arg) }