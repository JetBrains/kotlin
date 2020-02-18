// IGNORE_BACKEND: JVM_IR
// TODO KT-36812 Generate proper lifetime intervals for local variables in JVM_IR

fun test(): Char {
    val c: Char
    val l = Any()
    val l1 = Any()
    val l2 = Any()
    val l3 = Any()
    val l4 = Any()
    val l5 = Any()
    val l6 = Any()
    val l7 = Any()
    val l8 = Any()
    val l11 = Any()
    val l12 = Any()
    val l13 = Any()
    val l14 = Any()
    c = '1'
    return c
}

// 2 ISTORE 0
// 1 LOCALVARIABLE c C L1 L16 0
