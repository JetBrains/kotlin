// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: A.java
public class A {
    protected String foo() { return "OK"; }
}

// FILE: B.java
public interface B {
    static String foo() { return "Fail"; }
}

// FILE: box.kt
class C : A(), B {
    override fun foo(): String = super.foo()

    fun test(): String = foo()
}

fun box(): String = C().test()
