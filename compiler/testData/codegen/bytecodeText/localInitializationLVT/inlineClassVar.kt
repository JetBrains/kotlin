// IGNORE_BACKEND: JVM_IR

fun test(): UInt {
    var c: UInt
    run {
        c = 1u
    }
    return c
}

// 1 ASTORE 0
// 1 LOCALVARIABLE c Lkotlin/jvm/internal/Ref\$IntRef; L1 L.* 0