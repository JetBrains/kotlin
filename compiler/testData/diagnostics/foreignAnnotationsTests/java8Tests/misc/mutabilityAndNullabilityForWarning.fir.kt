// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations:warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: ReadOnly.java
package org.jetbrains.annotations;

public @interface ReadOnly {}

// FILE: J.java
import java.util.List;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ReadOnly;

public class J {
    @ReadOnly
    @Nullable
    public static List<String> foo() {
        return null;
    }
}

// FILE: main.kt
fun main() {
    takeMutable(<!ARGUMENT_TYPE_MISMATCH!>J.foo()<!>)
}

fun takeMutable(l: MutableList<String>) {}