package bitwise_demo

fun Long?.shl(bits : Int?) : Long = this!!.shl(bits!!)

fun box() : String {
    val x : Long? = 10
    val y : Int? = 12

    val result = x.shl(y)
    return if (result == 40960L) "OK" else result.toString()
}
