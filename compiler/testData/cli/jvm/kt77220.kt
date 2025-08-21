package p2

fun test() {
    val x = @DontMemoize {}
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class DontMemoize
