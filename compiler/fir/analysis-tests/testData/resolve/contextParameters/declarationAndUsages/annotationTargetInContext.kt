// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER
import kotlin.annotation.AnnotationTarget.TYPE

@Target(VALUE_PARAMETER)
annotation class AnnotationWithValueTarget

@Target(TYPE)
annotation class AnnotationWithTypeTarget

context(@AnnotationWithValueTarget a: @AnnotationWithTypeTarget String)
fun foo(): String {
    return a
}

context(@AnnotationWithValueTarget a: @AnnotationWithTypeTarget String)
val bar : String
    get() = a


context(<!WRONG_ANNOTATION_TARGET!>@AnnotationWithTypeTarget<!> a: <!WRONG_ANNOTATION_TARGET!>@AnnotationWithValueTarget<!> String)
fun baz(): String {
    return a
}

context(<!WRONG_ANNOTATION_TARGET!>@AnnotationWithTypeTarget<!> a: <!WRONG_ANNOTATION_TARGET!>@AnnotationWithValueTarget<!> String)
val qux : String
    get() = a
