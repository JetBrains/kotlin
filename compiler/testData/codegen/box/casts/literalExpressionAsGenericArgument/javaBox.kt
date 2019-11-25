// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: Box.java

public class Box<T> {
    private final T value;

    public Box(T value) {
        this.value = value;
    }

    public static <T> Box<T> create(T defaultValue) {
        return new Box(defaultValue);
    }

    public T getValue() {
        return value;
    }
}

// FILE: test.kt
// See KT-10313: ClassCastException with Generics

fun box(): String {
    val sub = Box<Long>(-1)
    return if (sub.value == -1L) "OK" else "fail"
}
