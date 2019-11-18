// IGNORE_BACKEND_FIR: JVM_IR
var entered = 0

fun <T> foo(t: T): T {
    entered++
    return t
}

fun box(): String {
    if (foo(null) == null) {}
    if (null == foo(null)) {}
    if (foo(null) == foo(null)) {}
    return if (entered == 4) "OK" else "Fail $entered"
}
