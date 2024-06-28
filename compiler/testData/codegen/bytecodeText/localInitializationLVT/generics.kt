inline fun <reified T> foo(default: T): T {
    val t: T
    run {
        t = default
    }
    return t
}

fun test() {
    foo(0.0f)
}

// JVM_TEMPLATES
// two in foo and two in test
// 4 ASTORE 2
// 1 LOCALVARIABLE t Ljava/lang/Object;
// 1 LOCALVARIABLE t\$iv Ljava/lang/Object;

// JVM_IR_TEMPLATES
// 4 ASTORE 2
// 1 LOCALVARIABLE t Ljava/lang/Object;
// 1 LOCALVARIABLE t\$iv Ljava/lang/Object;

// JVM_IR_TEMPLATES_WITH_INLINE_SCOPES
// 4 ASTORE 2
// 1 LOCALVARIABLE t Ljava/lang/Object;
// 1 LOCALVARIABLE t\\1 Ljava/lang/Object;
