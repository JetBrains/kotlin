// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_EXPRESSION

// FILE: p/J.java

package p;

public class J {
    public String s() { return null; }
}

// FILE: k.kt
import p.*

fun test(j: J) {
    j.s()?.length ?: ""
}

/* GENERATED_FIR_TAGS: elvisExpression, flexibleType, functionDeclaration, intersectionType, javaFunction, javaType,
nullableType, safeCall, stringLiteral */
