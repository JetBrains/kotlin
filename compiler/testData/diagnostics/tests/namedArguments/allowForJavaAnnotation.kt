// RUN_PIPELINE_TILL: BACKEND
// FILE: A.java

public @interface A {
    int x();

    String y();
}

// FILE: 1.kt

@A(x = 1, y = "2")
fun test() {}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaType, stringLiteral */
