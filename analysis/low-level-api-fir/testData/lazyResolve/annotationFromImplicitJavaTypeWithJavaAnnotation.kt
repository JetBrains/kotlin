// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// DISABLE_SEALED_INHERITOR_CALCULATOR
// FILE: main.kt
fun reso<caret>lveMe(i: JavaInterface) = i.id

// FILE: JavaClass.java
public interface JavaInterface {
    @KotlinAnnotation
    default int getId() {
        return -1;
    }
}

// FILE: KotlinAnnotation.kt
import java.lang.annotation.ElementType

@java.lang.annotation.Target(ElementType.TYPE_USE)
annotation class KotlinAnnotation