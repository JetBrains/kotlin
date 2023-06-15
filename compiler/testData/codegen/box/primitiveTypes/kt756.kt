package demo_range

operator fun Int?.unaryPlus() : Int = this!!.unaryPlus()
operator fun Int?.dec() : Int = this!!.dec()
operator fun Int?.inc() : Int = this!!.inc()
operator fun Int?.unaryMinus() : Int = this!!.unaryMinus()

fun box() : String {
    val x : Int? = 10
    val result = x?.inv()
    if (result == -11)// * x?.unaryPlus() * x?.dec() * x?.unaryMinus() as Number)
        return "OK"
    else
        return result.toString()
}
