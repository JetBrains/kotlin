// FILE: KotlinClass.kt
@Target(AnnotationTarget.TYPE)
annotation class MyAnno

class Kotlin<caret_useSite>Class

// FILE: A.java
public interface A {
    @MyAnno KotlinClass f<caret>oo();
}
