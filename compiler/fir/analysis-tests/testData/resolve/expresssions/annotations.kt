// WITH_STDLIB

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.EXPRESSION)
annotation class MyAnn

fun bar(x: Int) {}

fun foo() {
    <!WRONG_ANNOTATION_TARGET!>@MyAnn<!>
    val x: Int
    @MyAnn
    x = @MyAnn 42
    @MyAnn
    bar(@MyAnn x)

    val y = @MyAnn if (false) x else x
    val z = @MyAnn try {
        x
    } catch (t: Throwable) {
        0
    }
}
