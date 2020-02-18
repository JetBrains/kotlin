// IGNORE_BACKEND: JVM_IR
// TODO KT-36648 Captured variables not optimized in JVM_IR
// TODO KT-36812 Generate proper lifetime intervals for local variables in JVM_IR

fun test(): UInt {
    var c: UInt
    run {
        c = 1u
    }
    return c
}

// 1 ASTORE 0
// 1 LOCALVARIABLE c Lkotlin/jvm/internal/Ref\$IntRef; L1 L.* 0