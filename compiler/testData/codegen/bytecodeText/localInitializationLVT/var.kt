// IGNORE_BACKEND: JVM_IR

fun test(): Char {
    var c: Char
    run {
        c = ' '
    }
    return c
}

// 1 ASTORE 0
// 1 LOCALVARIABLE c Lkotlin/jvm/internal/Ref\$CharRef; L1 L10 0