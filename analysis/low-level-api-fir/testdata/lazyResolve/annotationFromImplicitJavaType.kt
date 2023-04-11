// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// DISABLE_SEALED_INHERITOR_CALCULATOR
// FILE: main.kt
fun resolveMe(i: JavaInterface) = i.id

// FILE: JavaClass.java
public interface JavaInterface {
    @KotlinAnnotation
    default int getId() {
        return -1;
    }
}

// FILE: KotlinAnnotation.kt
@Target(
    AnnotationTarget.TYPE
)
annotation class KotlinAnnotation