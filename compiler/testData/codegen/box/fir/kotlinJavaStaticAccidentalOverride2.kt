// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// IGNORE_CODEGEN_WITH_IR_FAKE_OVERRIDE_GENERATION
// Both K1 and K2 with IR f/o generator report ACCIDENTAL_OVERRIDE on B.foo (see also KT-60283)
// ISSUE: KT-59830

// FILE: A.java

public class A {
    public static void foo() {}
    public static void baz(String s) {}
}

// FILE: B.kt

open class B : A() {
}

// FILE: C.java

public class C extends B {
    public static void bar(int i) {}
}

// FILE: K.kt

open class K : C() {
    fun foo() {}
    fun foo(a: Any) {}
    fun bar(i: Int) {}
    fun bar(i: String) {}
    fun baz(i: Int) {}

    companion object {
        fun foo() {}
        fun bar(i: Int) {}
    }
}

fun box(): String {
    A.foo()
    A.baz("")
    C.bar(0)
    K.foo()
    K.bar(0)
    val k = K()
    k.foo()
    k.foo(0.0)
    k.bar(0)
    k.bar("")
    k.baz(0)
    return "OK"
}
