// FILE: Test.java

public abstract class Test<F> {
    protected final F value = null;
}

// FILE: test.kt

class A : Test<String>() {
    fun foo(): String? = value
}

fun box(): String {
    return if (A().foo() == null) "OK" else "Fail"
}
