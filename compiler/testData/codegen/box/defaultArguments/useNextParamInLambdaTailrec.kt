// IGNORE_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature. UNINITIALIZED_PARAMETER y. See KT-49800
tailrec fun foo(x: () -> String? = { y }, y: String = "fail"): String? {
    if (y == "start")
        return foo()
    return x()
}

fun box() = foo(y = "start") ?: "OK"
