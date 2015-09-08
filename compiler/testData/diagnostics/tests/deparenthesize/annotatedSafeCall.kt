@Target(AnnotationTarget.EXPRESSION)
annotation class foo

fun f(s : String?) : Boolean {
    return (@foo s?.equals("a"))!!
}