// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-13451

// FILE: J.java

public class J {
}

// FILE: Main.kt

class K {
    fun f() {}
}

fun test (j: J, k: K) {
    j == K::f
    j == k::f

    j === K::f
    j === k::f

    when (j) {
        k::f -> ""
        K::f -> ""
    }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, equalityExpression, functionDeclaration, javaType,
stringLiteral, whenExpression, whenWithSubject */
