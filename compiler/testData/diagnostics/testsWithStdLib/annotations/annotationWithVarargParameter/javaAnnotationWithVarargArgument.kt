// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.java
public @interface A {
    String[] value();
}

// FILE: b.kt
@A(*<!TYPE_MISMATCH!>arrayOf(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, "b")<!>)
fun test() {
}

/* GENERATED_FIR_TAGS: collectionLiteral, functionDeclaration, integerLiteral, javaType, stringLiteral */
