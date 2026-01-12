// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LATEST_LV_DIFFERENCE
// FILE: A.java
import org.jetbrains.annotations.*;

public class A {
    public void foo(int x) {}
    public void bar(@NotNull Double x) {}
}

// FILE: B.java
import org.jetbrains.annotations.*;
public class B extends A {
    public void foo(@NotNull Integer x) {}
    public void bar(double x) {}
}

// FILE: main.kt

fun foo(b: B) {
    // See KT-9182
    b.foo(1)
    b.bar(2.0)
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaType */
