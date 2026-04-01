// TARGET_BACKEND: JVM
// DUMP_IR
// FILE: box.kt

fun box(): String =
    foo(null)

fun foo(filter: ((String) -> Boolean)?): String {
    J(filter)
    return "OK"
}

// FILE: J.java

public class J {
    public J(Condition<? super String> filter) {
        if (filter != null) {
            filter.value("");
        }
    }
}

// FILE: Condition.java

public interface Condition<T> {
    boolean value(T t);
}
