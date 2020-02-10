// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// FILE: Test.java

public class Test {
    public static void callFoo() {
        new A().foo(null);
    }
}

// FILE: Test.kt

class A {
    fun foo(s: String) {}
}

fun box(): String {
    try {
        Test.callFoo()
        return "Fail 1"
    } catch (e: NullPointerException) {
        if (e.message != "Parameter specified as non-null is null: method A.foo, parameter s") {
            return "Fail 2 (message: ${e.message})"
        }
    } catch (e: Throwable) {
        return "Fail 3 (exception class: ${e::class.simpleName})"
    }
    return "OK"
}
