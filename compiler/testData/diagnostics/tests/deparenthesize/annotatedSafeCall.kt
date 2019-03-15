@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class foo

fun f(s : String?) : Boolean {
    return (@foo s?.equals("a"))!!
}