// !LANGUAGE: +ReferencesToSyntheticJavaProperties
// TARGET_BACKEND: JVM_IR

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