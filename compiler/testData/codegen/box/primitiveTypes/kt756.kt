// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package demo_range

operator fun Int?.unaryPlus() : Int = this!!.unaryPlus()
operator fun Int?.dec() : Int = this!!.dec()
operator fun Int?.inc() : Int = this!!.inc()
operator fun Int?.unaryMinus() : Int = this!!.unaryMinus()

fun box() : String {
    val x : Int? = 10
    System.out?.println(x?.inv())// * x?.unaryPlus() * x?.dec() * x?.unaryMinus() as Number)
    return "OK"
}
