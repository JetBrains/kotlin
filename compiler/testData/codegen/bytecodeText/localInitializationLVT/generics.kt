// IGNORE_BACKEND: JVM_IR

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

// 2 ASTORE 1
// 1 LOCALVARIABLE t\$iv Ljava/lang/Object; L2 L11 1