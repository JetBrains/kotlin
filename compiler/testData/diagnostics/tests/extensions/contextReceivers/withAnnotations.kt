// FIR_IDENTICAL
// !LANGUAGE: +ContextReceivers

class Context

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

