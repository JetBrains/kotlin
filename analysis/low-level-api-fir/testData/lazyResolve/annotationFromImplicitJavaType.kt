// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// FILE: main.kt
fun resol<caret>veMe(i: JavaInterface) = i.id

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