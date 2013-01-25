val Int.ext : () -> Int = { 5 }
val Long.ext : Long = 4.ext().toLong()  //(c.kt:4)
val y : Long = 10.toLong().ext

fun box() : String = if (y == 5.toLong()) "OK" else "fail"
