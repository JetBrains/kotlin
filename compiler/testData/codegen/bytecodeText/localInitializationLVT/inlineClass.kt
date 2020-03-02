// IGNORE_BACKEND: JVM_IR
// TODO KT-36648 Captured variables not optimized in JVM_IR

fun test(): UInt {
    val c: UInt
    run {
        c = 1u
    }
    return c
}

// 2 ISTORE 0
// 1 LOCALVARIABLE c I L1 L.* 0