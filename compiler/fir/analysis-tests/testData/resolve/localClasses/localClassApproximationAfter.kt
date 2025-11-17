// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ApproximateLocalTypesInPublicDeclarations
// ISSUE: KT-82454
// FIR_DUMP
// FILE: p/J.java
package p;

public class J {
    public static <T> T identity(T t) {
        return t;
    }
}
// FILE: test.kt
package p

interface Self<E>

class B {
    val x = run {
        class A : Self<A>
        J.identity(A())
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, flexibleType, interfaceDeclaration, javaFunction, lambdaLiteral, localClass,
nullableType, propertyDeclaration, typeParameter */
