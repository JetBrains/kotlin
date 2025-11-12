// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.Mutable:warn
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTICS_FULL_TEXT

// FILE: org/jetbrains/annotations/Mutable.java
package org.jetbrains.annotations;

public @interface Mutable {}

// FILE: J.java
import java.util.List;
import org.jetbrains.annotations.Mutable;

public class J {
    @Mutable
    public static List<String> foo() {
        return null;
    }

    public @Mutable List<String> bar() { return null; }
}

// FILE: main.kt
fun main() {
    takeReadonly(J.foo())
    J.foo().size
    for (x in J.foo()) {}
}

fun takeReadonly(l: List<String>) {}

abstract class K : J() {
    abstract <!WRONG_TYPE_FOR_JAVA_OVERRIDE!>override<!> fun bar(): List<String>
}
