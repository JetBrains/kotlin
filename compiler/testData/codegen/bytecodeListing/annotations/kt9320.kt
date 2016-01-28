annotation class Ann

@Ann open class My

@Target(AnnotationTarget.EXPRESSION)
annotation class AnnExpr

fun foo() {
    val v = @Ann @AnnExpr object: My() {}
    val w = @Ann @AnnExpr { v: My -> v.hashCode() }
}
