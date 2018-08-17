val Int.ext: () -> Int get() = { 5 }
val Long.ext: Long get() = 4.ext().toLong()  //(c.kt:4)
val y: Long get() = 10L.ext

fun box(): String = if (y == 5L) "OK" else "fail: $y"
