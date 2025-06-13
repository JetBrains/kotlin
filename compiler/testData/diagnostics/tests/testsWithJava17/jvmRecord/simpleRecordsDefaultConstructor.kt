// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// LANGUAGE: +JvmRecordSupport

// FILE: MyRecord.java
public record MyRecord(String str) {}

// FILE: main.kt

fun foo() {
    MyRecord("") // OK
    MyRecord<!NO_VALUE_FOR_PARAMETER!>()<!> // error
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
