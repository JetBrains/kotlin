// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
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

val a: @Nested(TypeAnnotation("Int" <!EVALUATED("IntAnno")!>+ "Anno"<!>)) Int = 1
val b: @NestedArray([TypeAnnotation("Element1" <!EVALUATED("Element1Anno")!>+ "Anno"<!>), TypeAnnotation("Element2" <!EVALUATED("Element2Anno")!>+ "Anno"<!>)]) Int = 1

// MODULE: main
// FILE: main.kt

fun box(): String {
    return "OK"
}
