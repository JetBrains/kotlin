// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!ARGUMENT_TYPE_MISMATCH!>arrayOf(1, "b")<!>)
fun test() {
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, stringLiteral */
