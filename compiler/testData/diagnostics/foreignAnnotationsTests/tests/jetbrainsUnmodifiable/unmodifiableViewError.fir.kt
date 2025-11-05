// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.UnmodifiableView:strict
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: org/jetbrains/annotations/UnmodifiableView.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
public @interface UnmodifiableView {}

// FILE: J.java
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.UnmodifiableView;

public class J {
    @UnmodifiableView
    public static List<String> foo() {
        return null;
    }

    public static void foo(Consumer<@UnmodifiableView List<String>> arg) {
    }
}

// FILE: main.kt
fun main() {
    takeMutable(<!ARGUMENT_TYPE_MISMATCH!>J.foo()<!>)
    J.foo().<!UNRESOLVED_REFERENCE!>add<!>("")
    J.foo().size
    for (x in J.foo()) {}

    J.foo { arg -> arg.<!UNRESOLVED_REFERENCE!>add<!>("") }
    J.foo <!ARGUMENT_TYPE_MISMATCH!>{ arg: MutableList<String> -> arg.add("") }<!>
}

fun takeMutable(l: MutableList<String>) {}
