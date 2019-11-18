// IGNORE_BACKEND_FIR: JVM_IR
import A.Nested

class A {
    class Nested {
        companion object {
            operator fun invoke(i: Int) = i
        }
    }
}

fun box() = if (Nested(42) == 42) "OK" else "fail"
