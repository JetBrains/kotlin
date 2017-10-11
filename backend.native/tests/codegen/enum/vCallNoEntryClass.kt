package codegen.enum.vCallNoEntryClass

import kotlin.test.*

enum class Zzz(val zzz: String, val x: Int) {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun toString(): String{
        return "('$zzz', $x)"
    }
}

@Test fun runTest() {
    println(Zzz.Z3.toString())
}