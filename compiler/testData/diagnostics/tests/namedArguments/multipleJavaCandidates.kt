// RUN_PIPELINE_TILL: FRONTEND
// FILE: A.java
public class A {
    public A(String b, String c) {
    }

    public A(String c) {
        this("", c);
    }
}

// FILE: main.kt
fun test() {
    <!NONE_APPLICABLE!>A<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>b<!> = "", <!DEBUG_INFO_MISSING_UNRESOLVED!>c<!> = "")
    A(<!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "", "")
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
