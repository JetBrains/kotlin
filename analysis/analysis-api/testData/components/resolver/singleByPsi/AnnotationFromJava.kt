// IGNORE_FE10
// RENDER_PSI_CLASS_NAME

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: MyRestrictedApi.java

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface MyRestrictedApi {
    String explanation();
    Class<? extends Annotation>[] allowlistAnnotations() default {};
    boolean allowedInTestonlyTargets() default false;
}

// MODULE: app(lib)
// FILE: main.kt

@Target(AnnotationTarget.FUNCTION) annotation class KAllowlist

class Test {
    @My<caret>RestrictedApi(
        explanation = "umbrella",
        allowlistAnnotations = [KAllowlist::class],
    )
    fun foo() {}
}
