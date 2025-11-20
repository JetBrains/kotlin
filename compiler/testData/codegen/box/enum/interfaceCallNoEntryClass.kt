// WITH_STDLIB

import kotlin.test.*

interface A {
    fun foo(): String
}

enum class Zzz(val zzz: String, val x: Int) : A {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun foo(): String{
        return "('$zzz', $x)"
    }
}

fun box(): String {
    assertEquals("('z3', 3)", Zzz.Z3.foo())
    val a: A = Zzz.Z3
    assertEquals("('z3', 3)", a.foo())
    return "OK"
}
