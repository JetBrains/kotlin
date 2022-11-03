// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class foo

fun f(s : String?) : Boolean {
    return (@foo s?.equals("a"))!!
}