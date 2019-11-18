// IGNORE_BACKEND_FIR: JVM_IR
enum class A {
    ONE,
    TWO
}

operator fun A.invoke(i: Int) = i

fun box() = if (A.ONE(42) == 42) "OK" else "fail"
