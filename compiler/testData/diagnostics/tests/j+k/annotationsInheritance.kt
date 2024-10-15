// DISABLE_JAVA_FACADE
// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// FILE: MyAnnotation.java
public @interface MyAnnotation {
}
// FILE: MyAnnoClass.java
public class MyAnnoClass implements MyAnnotation {
//...
}

// FILE: main.kt

val ann: MyAnnotation = MyAnnoClass()

fun foo(x: MyAnnoClass) {
    bar(x)
}

fun bar(y: MyAnnotation) {
    y.hashCode()
}
