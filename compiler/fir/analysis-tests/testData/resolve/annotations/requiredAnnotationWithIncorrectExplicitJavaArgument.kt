// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844
// FILE: JavaTarget.java
public enum JavaTarget {
    ANNOTATION_CLASS;
}

// FILE: MyAnnotation.kt
@Target(<!ARGUMENT_TYPE_MISMATCH!>JavaTarget.ANNOTATION_CLASS<!>)
annotation class MyAnnotation

/* GENERATED_FIR_TAGS: annotationDeclaration, javaProperty, javaType */
