// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-75844
// FILE: JavaTarget.java
public enum JavaTarget {
    ANNOTATION_CLASS;
}

// FILE: MyAnnotation.kt
import JavaTarget.ANNOTATION_CLASS

@Target(ANNOTATION_CLASS)
annotation class MyAnnot<caret>ation
