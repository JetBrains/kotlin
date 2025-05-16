annotation class Ann

@Ann open class My

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnExpr

fun foo() {
    val v = @Ann @AnnExpr object: My() {}
    val w = @Ann @AnnExpr { v: My -> v.hashCode() }
}
