// IGNORE_BACKEND_FIR: JVM_IR
fun foo(x: Int) = x

fun bar(x: Comparable<Int>) = if (x is Int) foo(x) else 0

fun box() = if (bar(42) == 42) "OK" else "Fail"
