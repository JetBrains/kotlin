// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: JavaClass.java
public class JavaClass extends KotlinClass {
    @KotlinAnnotation
    public static class NestedJavaClass {
    }
}

// FILE: main.kt
fun us<caret>age(pomTarget: JavaClass.NestedJavaClass) {

}

@KotlinAnnotation
open class KotlinClass

annotation class KotlinAnnotation
