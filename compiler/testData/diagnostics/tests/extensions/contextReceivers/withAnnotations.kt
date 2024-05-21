// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

class Context

class Receiver

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class MyAnnotation

context(Context)
@MyAnnotation
class ClassWithContext

class ClassWithoutContext {

    context(Context)
    @MyAnnotation
    fun memberFunctionWithContext() {}

    context(Context)
    @MyAnnotation
    val memberPropertyWithContext: Int get() = 10

}

context(Context)
@MyAnnotation
fun functionWithContext() {}

context(Context)
@MyAnnotation
val propertyWithContext: Int get() = 10

val functionalType: @MyAnnotation context(Context) () -> Unit = {}

val functionalTypeNullable: @MyAnnotation (context(Context) () -> Unit)? = {}

val functionalTypeWithReceiver: @MyAnnotation context(Context) Receiver.() -> Unit = {}