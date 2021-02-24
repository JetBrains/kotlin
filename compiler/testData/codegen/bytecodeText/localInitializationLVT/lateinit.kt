fun test(): Char {
    lateinit var c: Any
    run {
        c = ' '
    }
    return c as Char
}

// 1 LOCALVARIABLE c Ljava/lang/Object;
// 2 ASTORE 0