// TARGET_BACKEND: JVM
// ISSUE: KT-59461

// FILE: NullContainer.java
public interface NullContainer {
    static String NULL = null;
}

// FILE: main.kt
typealias NullableString = String?

fun updateThreadContext(): NullableString {
    return NullContainer.NULL
}

fun box(): String {
    updateThreadContext()
    return "OK"
}
