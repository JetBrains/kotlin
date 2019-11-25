// IGNORE_BACKEND_FIR: JVM_IR
class A {
    operator fun component1() = "O"
    operator fun component2(): String = throw RuntimeException("fail 0")
    operator fun component3() = "K"
}

fun foo(a: A, block: (A) -> String): String = block(a)

fun box() = foo(A()) { (x, _, y) -> x + y }
