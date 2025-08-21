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
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:NoTargets<!> val a: Int,
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:ComponentOnly<!> val b: Int,
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:TargetsOnlyInJava<!> val c: Int,
    @all:JavaParamComponent val d: Int,
    @all:JavaMethodComponent val e: Int,
) {
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:JavaParamComponent<!>
    val f get() = a + b
}

/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, annotationUseSiteTargetAll, classDeclaration,
collectionLiteral, data, getter, javaProperty, javaType, primaryConstructor, propertyDeclaration */
