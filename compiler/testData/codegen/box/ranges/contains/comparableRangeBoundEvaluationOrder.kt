// WITH_STDLIB
// KT-92004: evaluate range bounds before calling user-defined compareTo.

var log = ""
class C(val n: Int) : Comparable<C> {
    override fun compareTo(other: C): Int {
        log += "C$n:${other.n};"
        return n.compareTo(other.n)
    }
}
fun lower(): C { log += "L"; return C(1) }
fun upper(): C { log += "U"; return C(2) }

fun box(): String {
    val x = C(0)
    val result = x in lower()..upper()
    if (result) return "Wrong result"
    if (log != "LUC0:1;") return "Expected LUC0:1; but got $log"
    return "OK"
}
