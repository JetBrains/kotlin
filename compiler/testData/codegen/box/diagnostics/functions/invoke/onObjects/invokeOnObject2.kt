// IGNORE_BACKEND_FIR: JVM_IR
object A {
    operator fun invoke(i: Int) = i
}

fun box() = if (A(42) == 42) "OK" else "fail"