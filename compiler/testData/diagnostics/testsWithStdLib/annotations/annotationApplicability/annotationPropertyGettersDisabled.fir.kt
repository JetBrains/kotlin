// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ForbidJvmAnnotationsOnAnnotationParameters -ForbidFieldAnnotationsOnAnnotationParameters
// ISSUE: KT-70169

annotation class A(
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@get:JvmStatic<!> val x: Int,
    @get:JvmName("yy") val y: Int,
    <!SYNCHRONIZED_IN_ANNOTATION_WARNING!>@get:Synchronized<!> val z: Int,
    <!OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JvmOverloads<!> val v: Int,
    <!THROWS_IN_ANNOTATION_WARNING!>@get:Throws(Exception::class)<!> val w: Int,
    <!INAPPLICABLE_JVM_FIELD_WARNING, WRONG_ANNOTATION_TARGET_WARNING!>@JvmField<!> val r: Int,
    <!VOLATILE_ON_VALUE, WRONG_ANNOTATION_TARGET_WARNING!>@Volatile<!> val s: Int,
    <!WRONG_ANNOTATION_TARGET_WARNING!>@Transient<!> val t: Int,
)
