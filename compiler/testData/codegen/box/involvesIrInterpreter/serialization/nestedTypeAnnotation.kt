// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class TypeAnnotation(val str: String)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class Nested(val a: TypeAnnotation)

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.BINARY)
annotation class NestedArray(val a: Array<TypeAnnotation>)

val a: @Nested(TypeAnnotation(<!EVALUATED("IntAnno")!>"Int" + "Anno"<!>)) Int = 1
val b: @NestedArray([TypeAnnotation(<!EVALUATED("Element1Anno")!>"Element1" + "Anno"<!>), TypeAnnotation(<!EVALUATED("Element2Anno")!>"Element2" + "Anno"<!>)]) Int = 1

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
