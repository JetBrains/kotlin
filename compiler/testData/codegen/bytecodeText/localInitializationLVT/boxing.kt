// IGNORE_BACKEND: JVM_IR

fun test(): java.lang.Integer {
    val c: java.lang.Integer
    run {
        c = java.lang.Integer(1)
    }
    return c
}

// 2 ASTORE 0
// 1 LOCALVARIABLE c Ljava/lang/Integer; L1 L.* 0