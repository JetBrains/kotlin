// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
import kotlin.annotation.AnnotationTarget.*

@Target(TYPE, VALUE_PARAMETER)
annotation class Ann

@Target(TYPE, VALUE_PARAMETER, FUNCTION, PROPERTY)
annotation class AnnotationWithConstructor(val k: String)

context(@Ann a: @Ann String)
fun annotationOnContext() {}

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
fun annotationWithConstructor() {}

context(@Ann a: @Ann String)
val annotationOnContextProperty: String
    get() = ""

context(@AnnotationWithConstructor("") a: @AnnotationWithConstructor("") String)
val annotationWithConstructorProperty: String
    get() = ""

fun functionType(f: context(@Ann String) () -> Unit) {}

fun functionTypeWithConstructor(f: context(@AnnotationWithConstructor("") String) () -> Unit) {}