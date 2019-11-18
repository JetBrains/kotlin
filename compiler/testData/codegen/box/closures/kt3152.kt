// IGNORE_BACKEND_FIR: JVM_IR
public class Test {
    val content = 1
    inner class A {
        val v = object {
            fun f() = content
        }
    }
}

fun box(): String {
    Test().A()

    return "OK"
}