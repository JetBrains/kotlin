// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.Unmodifiable:warn
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: org/jetbrains/annotations/Unmodifiable.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE_USE})
public @interface Unmodifiable {}

// FILE: J.java
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.Unmodifiable;

public class J {
    @Unmodifiable
    public static List<String> foo() {
        return null;
    }

    public static void foo(Consumer<@Unmodifiable List<String>> arg) {
    }
}

// FILE: main.kt
fun main(l: List<String>) {
    takeMutable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.foo()<!>)
    <!RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.foo()<!>.add("")
    J.foo().size
    for (x in J.foo()) {}

    J.foo { arg -> <!RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>arg<!>.add("") }
    J.foo { arg: MutableList<String> -> arg.add("") }
}

fun takeMutable(l: MutableList<String>) {}
