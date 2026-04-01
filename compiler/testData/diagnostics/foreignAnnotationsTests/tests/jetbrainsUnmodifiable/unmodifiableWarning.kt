// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.Unmodifiable:warn
// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB

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

// FILE: B.java
import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

public class B<T extends @Unmodifiable List<String>> {
        public T getT() { return null; }
}

// FILE: main.kt
fun main() {
    takeMutable(J.foo())
    J.foo().add("")
    J.foo() += ""

    J.foo().size
    for (x in J.foo()) {}

    J.foo { arg -> arg.add("") }
    J.foo { arg: MutableList<String> -> arg.add("") }

    J.foo().toString()
    J.foo().asReversed()
    J.foo().asReversed().add("") // Unfortunately a false negative warning
}

fun takeMutable(l: MutableList<String>) {}

fun captured(b: B<*>) {
    b.t.add("")
}
