// IGNORE_BACKEND: JVM_IR
// TODO: JVM_IR uses ObjectRef instead of IntRef for the value

fun test(): UInt {
    val c: UInt
    run {
        c = 1u
    }
    return c
}

// 2 ISTORE 0
// 1 LOCALVARIABLE c I