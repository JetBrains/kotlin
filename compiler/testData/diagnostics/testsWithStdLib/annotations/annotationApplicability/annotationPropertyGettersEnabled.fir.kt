// LANGUAGE: +ForbidJvmAnnotationsOnAnnotationParameters
// ISSUE: KT-70169

annotation class A(
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@get:JvmStatic<!> val x: Int,
    @get:JvmName("yy") val y: Int,
    <!SYNCHRONIZED_IN_ANNOTATION_ERROR!>@get:Synchronized<!> val z: Int,
    <!OVERLOADS_WITHOUT_DEFAULT_ARGUMENTS, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JvmOverloads<!> val v: Int,
    @get:Throws(Exception::class) val w: Int,
)
