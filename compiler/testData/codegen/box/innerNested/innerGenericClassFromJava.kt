// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: JavaClass.java

public abstract class JavaClass {
    public static String test() {
        return Test.INSTANCE.foo(new Outer<String>("OK").new Inner<Integer>(1));
    }
}

// FILE: Kotlin.kt

class Outer<E>(val x: E) {
    inner class Inner<F>(val y: F) {
        fun foo() = x.toString() + y.toString()
    }
}

object Test {
    fun foo(x: Outer<String>.Inner<Integer>) = x.foo()
}

fun box(): String {
    val result = JavaClass.test()
    if (result != "OK1") return "Fail: $result"
    return "OK"
}
