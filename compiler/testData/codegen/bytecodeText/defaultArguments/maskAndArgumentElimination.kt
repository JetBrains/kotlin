inline fun test(p: String = "OK"): String {
    return p
}

fun box() : String {
    return test()
}

//mask check in test$default
// 1 IFEQ

//total ifs
// 1 IF

//no default argument on call site
// 0 NULL

//proper variable start label: after assignment
// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE p\\1 Ljava/lang/String; L2 L4 0

// JVM_TEMPLATES
// 1 LOCALVARIABLE p\$iv Ljava/lang/String; L2 L4 0
// 1 LDC "OK"\s*ASTORE 0\s*L2

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE p\$iv Ljava/lang/String; L2 L4 0
// 1 LDC "OK"\s*ASTORE 0\s*L2
