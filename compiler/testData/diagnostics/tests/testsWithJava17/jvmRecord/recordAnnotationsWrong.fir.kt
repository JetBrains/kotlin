// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +JvmRecordSupport +AnnotationAllUseSiteTarget +PropertyParamAnnotationDefaultTargetMode
// SKIP_TXT
// JVM_TARGET: 17
// ENABLE_JVM_PREVIEW
// DIAGNOSTICS: -DEPRECATED_JAVA_ANNOTATION

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
)

@JvmRecord
data class Else(
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:NoTargets<!> val a: Int,
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:ComponentOnly<!> val b: Int,
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@all:TargetsOnlyInJava<!> val c: Int,
)
