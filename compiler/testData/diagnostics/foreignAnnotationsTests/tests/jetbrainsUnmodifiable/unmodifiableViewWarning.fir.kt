// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.UnmodifiableView:warn
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

// FILE: B.java
import java.util.List;
import org.jetbrains.annotations.UnmodifiableView;

public class B<T extends @UnmodifiableView List<String>> {
    public T getT() { return null; }
}

// FILE: main.kt
fun main() {
    takeMutable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.foo()<!>)
    <!RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.foo()<!>.add("")
    J.foo().size
    for (x in J.foo()) {}

    J.foo { arg -> <!RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>arg<!>.add("") }
    J.foo { arg: MutableList<String> -> arg.add("") }
}

fun takeMutable(l: MutableList<String>) {}

fun captured(b: B<*>) {
    <!RECEIVER_MUTABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>b.t<!>.add("")
}
