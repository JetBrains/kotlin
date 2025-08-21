// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// ISSUE: KT-75844
// FILE: JavaTarget.java
public enum JavaTarget {
    ANNOTATION_CLASS;
}

// FILE: MyAnnotation.kt
@Target(JavaTarget.ANNOTATION_CLASS)
annotation class MyA<caret>notation
