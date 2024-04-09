// SKIP_TXT
// ISSUE: KT-54521
// !LANGUAGE: -EnhanceNullabilityOfPrimitiveArrays
// FILE: J.java
import org.jetbrains.annotations.Nullable;

public interface J {
    int @Nullable [] foo();
}

// FILE: main.kt
fun bar(j: J) = j.foo()<!UNSAFE_CALL!>.<!>iterator()

fun baz(j: J) = <!UNSAFE_CALL!>j.foo()[0]<!>
