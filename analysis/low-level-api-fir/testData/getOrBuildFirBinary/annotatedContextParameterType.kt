// LANGUAGE: +ContextParameters
// DECLARATION_TYPE: org.jetbrains.kotlin.psi.KtClass
// MAIN_FILE_NAME: MyClass

// FILE: MyAnnotation.kt
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
annotation class MyAnnotation

// FILE: ParameterizedInterface.kt
interface ParameterizedInterface<T>

// FILE: MyClass.kt
class MyClass {
    context(@MyAnnotation _: @MyAnnotation ParameterizedInterface<@MyAnnotation String>)
    fun foo() {

    }

    context(@MyAnnotation _: @MyAnnotation ParameterizedInterface<@MyAnnotation String>)
    val bar: String get() = "str"
}
