// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// Reason: red code
// ISSUE: KT-63242, KT-66324

// FILE: box.kt

private open class C : B() {
    override fun foo(d: Any?): String = "FAIL"
}

fun box(): String =
    D().foo("s")

// FILE: A.java
interface A {
    String foo(Object value);
}

abstract class B implements A {
    public String foo(String value) {
        return "OK";
    }
}

class D extends C {
}
