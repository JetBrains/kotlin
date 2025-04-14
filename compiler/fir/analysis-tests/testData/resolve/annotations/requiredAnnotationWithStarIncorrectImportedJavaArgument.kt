// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844
// FILE: JavaTarget.java
public enum JavaTarget {
    ANNOTATION_CLASS;
}

// FILE: MyAnnotation.kt
import JavaTarget.*

@Target(<!ARGUMENT_TYPE_MISMATCH!>ANNOTATION_CLASS<!>)
annotation class MyAnnotation
