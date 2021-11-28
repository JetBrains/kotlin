// WITH_STDLIB

fun box(): String {
    val x: Array<Array<*>> = arrayOf(arrayOf(0.plus(-1L).mod(5.mod(-47)).rem(1)))
    return "OK"
}
