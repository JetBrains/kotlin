// FIR_IDENTICAL
// SKIP_TXT
// FILE: I1.java
import org.checkerframework.checker.nullness.qual.NonNull;

public interface I1<@NonNull T> { }

// FILE: I2.java
public interface I2<T extends I1<T>> { }

// FILE: main.kt
fun foo(): I2<*> = TODO()
