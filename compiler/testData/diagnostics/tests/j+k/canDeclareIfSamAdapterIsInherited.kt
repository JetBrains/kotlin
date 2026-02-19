// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: Super.java
public class Super {
    void foo(Runnable r) {
    }
}

// FILE: Sub.kt
class Sub() : Super() {
    fun foo(r : (() -> Unit)?) {
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, javaType, nullableType, primaryConstructor */
