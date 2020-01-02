@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class special

@Target(AnnotationTarget.TYPE)
annotation class base

fun transform(i: Int, tr: (@special Int) -> Int): Int = @special tr(@special i)

fun foo(i: Int): Int {
    val j = @special i + 1
    if (j == 1) return foo(@special 42)
    return transform(@special j, @special { i: @base Int -> @base i * 2 })
}
