// ISSUE: KT-69058

// FILE: JavaPropertyAnnotation.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@kotlin.annotation.Target(
    allowedTargets = {
        kotlin.annotation.AnnotationTarget.PROPERTY,
    }
)
public @interface JavaPropertyAnnotation {}

// FILE: test.kt

class Foo(@JavaPropertyAnnotation val y: Int) {
    @JavaPropertyAnnotation val x = 1

    fun foo() {}
}
