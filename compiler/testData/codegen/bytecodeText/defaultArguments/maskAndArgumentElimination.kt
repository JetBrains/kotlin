// IGNORE_BACKEND: JVM_IR
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
// 1 LOCALVARIABLE p\$iv Ljava/lang/String; L2 L4 0
// 1 LDC "OK"\s*ASTORE 0\s*L2