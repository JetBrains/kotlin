// IGNORE_BACKEND_FIR: JVM_IR
import A.ONE

enum class A {
    ONE,
    TWO;

    operator fun invoke(i: Int) = i
}

fun box() = if (ONE(42) == 42) "OK" else "fail"
