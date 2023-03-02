// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// K1_STATUS: Broken because of KT-57103

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
