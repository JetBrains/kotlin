// FIR_IDENTICAL
// FULL_JDK
// !LANGUAGE: +RepeatableAnnotations

//import java.lang.annotation.*

typealias Rep = java.lang.annotation.Repeatable

<!REPEATABLE_CONTAINER_MUST_HAVE_VALUE_ARRAY_ERROR!>@Rep(RepeatableAnnotationContainer::class)<!>
annotation class RepeatableAnnotation
annotation class RepeatableAnnotationContainer

@RepeatableAnnotation class Annotated
