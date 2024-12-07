package demo_long

fun Long?.inv() : Long = this!!.inv()

fun box() : String {
    val x : Long? = 10
    val result = x.inv()
    return if(result == -11.toLong()) "OK" else result.toString()
}
