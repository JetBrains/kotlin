// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-76615

// FILE: Wrapper.java
public final class Wrapper<T> {
    private final T value;

    public Wrapper(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }
}

// FILE: main.kt
fun box(): String {
    val v = Wrapper(10.toUByte())
    val data = ubyteArrayOf(v.value)
    return if (data[0] == 10.toUByte()) {
        "OK"
    } else {
        "Fail: $data"
    }
}
