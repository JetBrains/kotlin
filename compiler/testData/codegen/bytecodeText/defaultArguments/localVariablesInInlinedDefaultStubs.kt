inline fun test(p: String = "OK"): String {
    var x = 1
    return p
}

fun box() : String {
    return test()
}

// No $iv suffix on LVT entries in test and test$default
// 2 LOCALVARIABLE p Ljava/lang/String;
// 2 LOCALVARIABLE x I

// JVM_TEMPLATES
// The $iv suffix should be present in box
// 1 LOCALVARIABLE p\$iv Ljava/lang/String;
// 1 LOCALVARIABLE x\$iv I

// JVM_IR_TEMPLATES
// 1 LOCALVARIABLE p\$iv Ljava/lang/String;
// 1 LOCALVARIABLE x\$iv I

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 1 LOCALVARIABLE p\\1 Ljava/lang/String;
// 1 LOCALVARIABLE x\\1 I
