// FIR_IDENTICAL
// DUMP_CFG: LEVELS

object A {
    val a = args[1]
}

val rv = A.a + B.b

object B {
    val b = args[2]
}
