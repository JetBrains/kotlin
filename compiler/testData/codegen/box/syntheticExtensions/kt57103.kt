// !LANGUAGE: +ReferencesToSyntheticJavaProperties
// TARGET_BACKEND: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-62849

// FILE: J.java

public class J<T> {
    private final T value;
    public J(T value) {
        this.value = value;
    }
    public T getValue() {
        return value;
    }

    public T foo() {
        return value;
    }
}

// FILE: test.kt

fun box(): String {
    val j = J("OK")
    return run(j::value)
}