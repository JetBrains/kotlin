annotation class base

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class special

fun transform(i: Int, tr: (Int) -> Int): Int = @base @special tr(@special i)

@base @special fun foo(i: Int): Int {
    val j = @base @special i + 1
    if (j == 1) return foo(@special @base 42)
    return transform(@special j, @base @special { @special it * 2 })
}
