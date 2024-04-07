// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// Reason: incorrect result FAIL B
// IGNORE_LIGHT_ANALYSIS
// ISSUE: KT-63242, KT-66324

// FILE: box.kt

private open class C : B() {
    override fun foo(d: Any?): String = "FAIL C"
}

fun box(): String =
    D().foo("s")

// FILE: A.java
interface A {
    String foo(Object value);
}

abstract class B implements A {
    public String foo(String value) {
        return "FAIL B";
    }
}

class D extends C {
    @Override
    public String foo(Object value) {
        return "OK";
    }
}
