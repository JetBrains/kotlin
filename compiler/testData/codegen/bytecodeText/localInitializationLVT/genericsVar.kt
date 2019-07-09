// IGNORE_BACKEND: JVM_IR

inline fun <reified T> foo(default: T): T {
    var t: T
    run {
        t = default
    }
    return t
}

fun test() {
    foo(0.0f)
}

// two in foo and two in test

// 2 ASTORE 2
// 1 LOCALVARIABLE t\$iv Lkotlin/jvm/internal/Ref\$ObjectRef; L3 L.* 2