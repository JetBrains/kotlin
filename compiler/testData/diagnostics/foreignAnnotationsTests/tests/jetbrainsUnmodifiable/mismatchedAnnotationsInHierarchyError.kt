// FULL_JDK
// NULLABILITY_ANNOTATIONS: @org.jetbrains.annotations.Unmodifiable:strict

// FILE: org/jetbrains/annotations/Unmodifiable.java
package org.jetbrains.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE_USE})
public @interface Unmodifiable {}

// FILE: A.java
import org.jetbrains.annotations.Unmodifiable;
import java.util.List;

public class A {
    public @Unmodifiable List<String> foo() { return null; }
}

// FILE: B.java
import org.jetbrains.annotations.Unmodifiable;
import java.util.List;

public class B extends A {
    @Override public List<String> foo() { return null; }
}

// FILE: test.kt
fun test(b: B) {
    val list: MutableList<String> = b.<!INITIALIZER_TYPE_MISMATCH!>foo<!>()
}
