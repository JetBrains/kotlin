// IGNORE_BACKEND_K1: JVM_IR
// JVM_ABI_K1_K2_DIFF: KT-75944
annotation class Ann

@Ann open class My

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnExpr

fun foo() {
    val v = @Ann @AnnExpr object: My() {}
    val w = @Ann @AnnExpr { v: My -> v.hashCode() }
}
