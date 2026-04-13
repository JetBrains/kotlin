// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-62554
// FIR_DUMP
// SCOPE_DUMP: C:foo
// FILE: A.java

import org.jetbrains.annotations.*;

public class A {
    public void foo(@NotNull Integer x) {}
}

// FILE: main.kt

interface B {
    fun foo(x: Int) {}
}

class C : A(), B

fun main() {
    C().foo(42)
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, interfaceDeclaration, javaType */
