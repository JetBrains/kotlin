private const val <caret>TAG = "Tagged"

annotation class InAnn(val value: String)

@InAnn(TAG) class AnnHolder

@InAnn(value = TAG) class AnotherAnnHolder

@InAnn("This is $TAG") class ComplexAnnHolder {
    @InAnn("That is $TAG") fun foo() {}
}

@InAnn("This is also $TAG") fun bar() {}

fun baz(@InAnn("This is $TAG too") x: Int) {}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExprAnn(val value: String)

val inProperty = TAG

fun foo() {
    @InAnn("Local $TAG") val x = @ExprAnn(TAG) inProperty
}