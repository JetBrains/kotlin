// IS_APPLICABLE: false

fun foo(y: Int, z: Int) = y - z

val x = { second: Int, first: Int -> foo(first, second) }