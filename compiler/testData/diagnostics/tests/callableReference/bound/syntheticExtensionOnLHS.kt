// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: A.java

class A {
    public CharSequence getFoo() { return null; }
}

// FILE: test.kt

fun test() {
    with (A()) {
        foo::toString
    }
}

/* GENERATED_FIR_TAGS: callableReference, flexibleType, functionDeclaration, javaFunction, javaProperty, javaType,
lambdaLiteral */
