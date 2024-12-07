// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ForbidJvmAnnotationsOnAnnotationParameters +ForbidFieldAnnotationsOnAnnotationParameters
// ISSUE: KT-70169

annotation class A(
    @get:JvmStatic val x: Int,
    @get:JvmName("yy") val y: Int,
    @get:Synchronized val z: Int,
    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JvmOverloads<!> val v: Int,
    @get:Throws(Exception::class) val w: Int,
    @JvmField val r: Int,
    <!VOLATILE_ON_VALUE!>@Volatile<!> val s: Int,
    @Transient val t: Int,
)
