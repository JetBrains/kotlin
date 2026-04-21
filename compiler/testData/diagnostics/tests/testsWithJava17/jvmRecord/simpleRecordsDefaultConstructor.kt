// RUN_PIPELINE_TILL: FRONTEND

// FILE: MyRecord.java
public record MyRecord(String str) {}

// FILE: main.kt

fun foo() {
    MyRecord("") // OK
    <!NO_VALUE_FOR_PARAMETER!>MyRecord<!>() // error
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
