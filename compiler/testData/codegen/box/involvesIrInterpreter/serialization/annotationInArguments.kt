// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Annotation(val str: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithAnnotation(val anno: Annotation)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AnnotationWithAnnotationWithAnnotation(val anno: AnnotationWithAnnotation)

@AnnotationWithAnnotation(Annotation("Str" + "ing"))
class A

@AnnotationWithAnnotationWithAnnotation(AnnotationWithAnnotation(Annotation("Str" + "ing")))
class B

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}