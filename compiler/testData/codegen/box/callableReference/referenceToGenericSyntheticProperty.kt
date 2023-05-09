// TARGET_BACKEND: JVM
// !LANGUAGE: +ReferencesToSyntheticJavaProperties

// FILE: J.java

public class J<T> {
    private final T value;
    public J(T value) {
        this.value = value;
    }
    public T getValue() {
        return value;
    }
}


// FILE: test.kt

fun box(): String {
    val j = J("OK")
    if (j.value != "OK") return "FAIL"
    if (run(j::value) != "OK") return "FAIL"
    if (j.let(J<String>::value) != "OK") return "FAIL"

    return "OK"
}
