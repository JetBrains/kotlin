fun test(): Char {
    lateinit var c: Any
    run {
        c = ' '
    }
    return c as Char
}

// 1 LOCALVARIABLE c Ljava/lang/Object;

// JVM_TEMPLATES
// 2 ASTORE 0

// JVM_IR_TEMPLATES
// 3 ASTORE 0
// *two* of them are after the start of c's live range