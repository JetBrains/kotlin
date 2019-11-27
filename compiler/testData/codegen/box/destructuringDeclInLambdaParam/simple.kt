// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: String, val y: String)

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() = foo(A("O", "K")) { (x, y) -> x + y }
