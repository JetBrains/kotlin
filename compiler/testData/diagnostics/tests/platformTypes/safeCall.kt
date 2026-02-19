// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// FILE: p/J.java

package p;

public class J {
    public String s() { return null; }
}

// FILE: k.kt
import p.*

fun test(j: J) {
    j.s()?.length.checkType { _<Int?>() }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, javaFunction, javaType, lambdaLiteral, nullableType, safeCall, typeParameter, typeWithExtension */
