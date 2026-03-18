// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-84310

// FILE: MyJavaEnum.java

public enum MyJavaEnum {
    GOOD_JAVA_VALUE,
    @Deprecated
    DEPRECATED_JAVA_VALUE
}

// FILE: main.kt

fun test() {
    MyJavaEnum.GOOD_JAVA_VALUE
    MyJavaEnum.<!DEPRECATION!>DEPRECATED_JAVA_VALUE<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaProperty, javaType */
