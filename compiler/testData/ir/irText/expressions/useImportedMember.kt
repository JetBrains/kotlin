import C.f
import C.p
import C.ext
import C.g1
import C.g2
import C.fromClass
import C.fromInterface
import C.genericFromSuper

interface I<G> {
    fun <T> T.fromInterface(): T = this

    fun genericFromSuper(g: G) = g
}

open class BaseClass {
    val <T> T.fromClass: T
        get() = this
}

object C: BaseClass(), I<String> {
    fun f(s: Int) = 1
    fun f(s: String) = 2
    fun Boolean.f() = 3

    var p: Int = 4
    val Int.ext: Int
        get() = 6

    fun <T> g1(t: T): T = t
    val <T> T.g2: T
        get() = this
}

fun box(): String {
    if (f(1) != 1) return "1"
    if (f("s") != 2) return "2"
    if (true.f() != 3) return "3"
    if (p != 4) return "4"
    p = 5
    if (p != 5) return "5"
    if (5.ext != 6) return "6"
    if (g1("7") != "7") return "7"
    if ("8".g2 != "8") return "8"
    if (9.fromInterface() != 9) return "9"
    if ("10".fromClass != "10") return "10"
    if (genericFromSuper("11") != "11") return "11"

    return "OK"
}
