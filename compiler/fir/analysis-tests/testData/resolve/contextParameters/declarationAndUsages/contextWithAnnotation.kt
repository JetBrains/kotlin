// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
import kotlin.annotation.AnnotationTarget.*

@Target(TYPE, VALUE_PARAMETER)
annotation class Ann

@Target(VALUE_PARAMETER)
annotation class AnnotationWithValueTarget

@Target(TYPE)
annotation class AnnotationWithTypeTarget

@Target(TYPE, VALUE_PARAMETER, FUNCTION, PROPERTY)
annotation class AnnotationWithConstructor(val k: String)

context(@Ann @AnnotationWithValueTarget a: @Ann @AnnotationWithTypeTarget String)
fun moreThenOneAnnotation() {}

context(@Ann a: @Ann String, @Ann b: @Ann String)
fun moreThenOneContextWithAnnotation() {}

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
fun annotationWithConstructor() {}

context(@Ann @AnnotationWithValueTarget a: @Ann @AnnotationWithTypeTarget String)
val moreThenOneAnnotationProperty: String
    get() = a

context(@Ann a: @Ann String, @Ann b: @Ann String)
val moreThenOneContextWithAnnotationProperty: String
    get() = a

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
val annotationWithConstructorProperty: String
    get() = a