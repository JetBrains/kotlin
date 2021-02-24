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
// 4 ASTORE 2
// 1 LOCALVARIABLE t Ljava/lang/Object;
// 1 LOCALVARIABLE t\$iv Ljava/lang/Object;