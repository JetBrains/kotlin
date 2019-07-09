// IGNORE_BACKEND: JVM_IR

fun test(): Char {
    lateinit var c: Any
    run {
        c = ' '
    }
    return c as Char
}

// 2 ASTORE 0
// 1 LOCALVARIABLE c Ljava/lang/Object; L1 L.* 0