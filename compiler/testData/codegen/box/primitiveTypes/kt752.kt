// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

package demo_range

operator fun Int?.rangeTo(other : Int?) : IntRange = this!!.rangeTo(other!!)

fun box() : String {
    val x : Int? = 10
    val y : Int? = 12

    for (i in x..y)
      System.out?.println(i.inv())
    return "OK"
}
