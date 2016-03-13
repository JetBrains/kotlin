// WITH_RUNTIME
// Basically this test checks that no captured type used as argument for signature mapping
class SwOperator<T>: Operator<List<T>, T>

interface Operator<R, T>

open class Inv<T>

class Obs<Y> {
    inline fun <reified X> lift(lift: Operator<out X, in Y>) = object : Inv<X>() {}
}

fun box(): String {
    val o: Obs<CharSequence> = Obs()

    val inv = o.lift(SwOperator())
    val signature = inv.javaClass.genericSuperclass.toString()

    if (signature != "Inv<java.util.List<?>>") return "fail 1: $signature"

    return "OK"
}
