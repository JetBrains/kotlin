fun test(): java.lang.Integer {
    val c: java.lang.Integer
    run {
        c = java.lang.Integer(1)
    }
    return c
}

// 2 ASTORE 0

// JVM_TEMPLATES
// 1 LOCALVARIABLE c Ljava/lang/Integer;

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE c Ljava/lang/Object;