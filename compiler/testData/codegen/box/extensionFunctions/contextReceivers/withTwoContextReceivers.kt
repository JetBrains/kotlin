// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class A { val o = "O" }
class B { val k = "K" }

val A.bar get() = o

context(A, B)
fun ok() = bar + k

fun box(): String = with(A()) { with(B()) { ok() } }
