// IGNORE_BACKEND: JVM_IR

fun test(): Char {
    val c: Char
    run {
        c = ' '
        println(c)
    }
    return c
}

// The first on declaration, the other on initialization
// 2 ISTORE 0
// 1 LOCALVARIABLE c C L1 L.* 0