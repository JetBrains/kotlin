// FIR_IDENTICAL
// WITH_RUNTIME

// FILE: Java.java
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Java<T extends @Nullable Object> {
    @NonNull T getFoo();
}

// FILE: main.kt
fun usingMethod(java : Java<String?>) : String = java.getFoo()
fun usingProperty(java : Java<String?>) : String = java.foo
