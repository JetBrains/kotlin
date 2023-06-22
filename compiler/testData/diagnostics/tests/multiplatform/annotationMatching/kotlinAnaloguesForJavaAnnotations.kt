// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Retention(AnnotationRetention.RUNTIME)
expect annotation class JavaTypealiasAnnotationAnalogue

@Retention(AnnotationRetention.RUNTIME)
expect annotation class JavaTypealiasKotlinAnnotation

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual typealias JavaTypealiasAnnotationAnalogue = JavaTypealiasAnnotationAnalogueImpl

actual typealias JavaTypealiasKotlinAnnotation = JavaTypealiasKotlinAnnotationImpl

// FILE: jvmJavaImpls.java
import kotlin.annotation.AnnotationRetention;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface JavaTypealiasAnnotationAnalogueImpl {
}

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
public @interface JavaTypealiasKotlinAnnotationImpl {
}
