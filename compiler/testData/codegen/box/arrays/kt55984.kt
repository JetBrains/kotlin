// WITH_STDLIB

fun box() : String{
    for (iter in 0 until 10) {
        val destination = ByteArray(8)
        if (destination[0] != 0.toByte()) return "FAIL"
        destination[0] = 1
    }
    return "OK"
}
