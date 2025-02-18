// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +JvmRecordSupport +AnnotationAllUseSiteTarget +PropertyParamAnnotationDefaultTargetMode
// SKIP_TXT
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW
// DIAGNOSTICS: -DEPRECATED_JAVA_ANNOTATION

// FILE: JavaParamComponent.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
public @interface JavaParamComponent {
}

// FILE: JavaMethodComponent.java

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.RECORD_COMPONENT, ElementType.METHOD })
public @interface JavaMethodComponent {
}

// FILE: test.kt

@Target()
annotation class NoTargets

@java.lang.annotation.Target(java.lang.annotation.ElementType.RECORD_COMPONENT)
@Target()
annotation class ComponentOnly

@java.lang.annotation.Target(value = [ java.lang.annotation.ElementType.RECORD_COMPONENT, java.lang.annotation.ElementType.FIELD ])
@Target()
annotation class TargetsOnlyInJava

@JvmRecord
data class Some(
    <!WRONG_ANNOTATION_TARGET!>@NoTargets<!> val a: Int,
    <!WRONG_ANNOTATION_TARGET!>@ComponentOnly<!> val b: Int,
    <!WRONG_ANNOTATION_TARGET!>@TargetsOnlyInJava<!> val c: Int,
    @JavaParamComponent val d: Int,
    <!WRONG_ANNOTATION_TARGET!>@JavaMethodComponent<!> val e: Int,
) {
    <!WRONG_ANNOTATION_TARGET!>@JavaParamComponent<!>
    val f get() = a + b
}

@JvmRecord
data class Else(
    <!UNSUPPORTED_FEATURE!>@all:NoTargets<!> val a: Int,
    <!UNSUPPORTED_FEATURE!>@all:ComponentOnly<!> val b: Int,
    <!UNSUPPORTED_FEATURE!>@all:TargetsOnlyInJava<!> val c: Int,
    <!UNSUPPORTED_FEATURE!>@all:JavaParamComponent<!> val d: Int,
    <!UNSUPPORTED_FEATURE!>@all:JavaMethodComponent<!> val e: Int,
) {
    <!UNSUPPORTED_FEATURE!>@all:JavaParamComponent<!>
    val f get() = a + b
}
