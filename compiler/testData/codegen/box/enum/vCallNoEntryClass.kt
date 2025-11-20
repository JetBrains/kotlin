// WITH_STDLIB

import kotlin.test.*

enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun toString(): String{
        return "('$zzz', $x)"
    }
}

fun box(): String {
    assertEquals("('z3', 3)", Zzz.Z3.toString())
    return "OK"
}
