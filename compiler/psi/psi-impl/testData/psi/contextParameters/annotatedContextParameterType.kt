// LANGUAGE: +ContextParameters
// FILE: MyAnnotation.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class MyAnnotation

// FILE: ParameterizedInterface.kt
interface ParameterizedInterface<T>

// FILE: main.kt
context(@MyAnnotation _: @MyAnnotation ParameterizedInterface<@MyAnnotation String>)
fun foo() {

}

context(@MyAnnotation _: @MyAnnotation ParameterizedInterface<@MyAnnotation String>)
val bar: String get() = "str"
