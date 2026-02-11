// LANGUAGE: +ContextReceivers, -ContextParameters
// IGNORE_BACKEND_K2: ANY
// IGNORE_HEADER_MODE: ANY
// TARGET_BACKEND: JVM_IR

class A { val o = "O" }
class B { val k = "K" }

val A.bar get() = o

context(A, B)
fun ok() = bar + k

fun box(): String = with(A()) { with(B()) { ok() } }
