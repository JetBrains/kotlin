// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_FIR_DIAGNOSTICS
// reason: red code
// ISSUE: KT-63242, KT-66324

// FILE: A.java
interface A {
    String foo(Object value);
}

// FILE: B.java
abstract class B implements A {
    public String foo(String value) {
        return "OK";
    }
}

// FILE: C.kt
private open class C : B() {
    override fun foo(d: Any?): String = "FAIL"
}

// FILE: D.java
class D extends C {}

// FILE: box.kt
fun box(): String = <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>D()<!>.foo("")
