// SKIP_TXT
// !LANGUAGE: -EnhanceNullabilityOfPrimitiveArrays
// FILE: J.java
import org.jetbrains.annotations.Nullable;

public interface J {
    int @Nullable [] foo();
}

// FILE: main.kt
fun bar(j: J) = j.foo()<!UNSAFE_CALL!>.<!>iterator()
