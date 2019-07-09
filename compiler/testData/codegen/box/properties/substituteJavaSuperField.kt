// TARGET_BACKEND: JVM

// FILE: Test.java

public abstract class Test<F> {
    protected final F value = null;
}

// FILE: test.kt
// See KT-5445: Bad access to protected data in getfield

class A : Test<String>() {
    fun foo(): String? = value
    fun bar(): String? = this.value
}

fun box(): String {
    if (A().foo() != null) return "Fail 1"
    if (A().bar() != null) return "Fail 2"
    return "OK"
}
