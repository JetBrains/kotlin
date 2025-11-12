// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.Unmodifiable:strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: org/jetbrains/annotations/Unmodifiable.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
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

// FILE: B.java
import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

public class B<T extends @Unmodifiable List<String>> {
    public T getT() { return null; }
}

// FILE: main.kt
fun main() {
    takeMutable(<!TYPE_MISMATCH!>J.foo()<!>)
    J.foo().<!UNRESOLVED_REFERENCE!>add<!>("")
    J.foo().size
    for (x in J.foo()) {}

    J.foo { arg -> arg.<!UNRESOLVED_REFERENCE!>add<!>("") }
    J.foo <!TYPE_MISMATCH!>{ <!EXPECTED_PARAMETER_TYPE_MISMATCH!>arg: MutableList<String><!> -> arg.add("") }<!>
}

fun takeMutable(l: MutableList<String>) {}

fun captured(b: B<*>) {
    b.t.<!UNRESOLVED_REFERENCE!>add<!>("")
}
