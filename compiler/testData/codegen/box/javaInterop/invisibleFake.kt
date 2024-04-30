// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: JVM_IR
// reason: incorrect result (FAIL B)
// ISSUE: KT-63242, KT-66324

// FILE: A.java
interface A {
    String foo(Object value);
}

// FILE: B.java
abstract class B implements A {
    public String foo(String value) {
        return "FAIL B";
    }
}

// FILE: C.kt
private open class C : B() {
    override fun foo(d: Any?): String = "FAIL C"
}

// FILE: D.java
class D extends C {
    @Override
    public String foo(Object value) {
        return "OK";
    }
}

// FILE: box.kt
fun box(): String = D().foo("")
