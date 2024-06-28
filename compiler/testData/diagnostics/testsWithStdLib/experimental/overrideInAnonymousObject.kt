// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class MyAnn

interface MyInterface {
    @MyAnn
    fun foo()

    @MyAnn
    fun bar()
}

val field = object : MyInterface {
    @MyAnn
    override fun foo() {}

    @OptIn(MyAnn::class)
    override fun bar() {}
}
