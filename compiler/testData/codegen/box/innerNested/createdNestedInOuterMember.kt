// IGNORE_BACKEND_FIR: JVM_IR
fun foo(f: (Int) -> Int) = f(0)

class Outer {
    class Nested {
        val y = foo { a -> a }
    }

    fun bar(): String {
        val a = Nested()
        return "OK"
    }
}

fun box() = Outer().bar()
