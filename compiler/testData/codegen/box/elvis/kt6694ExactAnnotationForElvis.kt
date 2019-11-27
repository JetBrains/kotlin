// IGNORE_BACKEND_FIR: JVM_IR
interface PsiElement {
    fun <T: PsiElement> findChildByType(i: Int): T? =
            if (i == 42) JetOperationReferenceExpression() as T else throw Exception()
}
interface JetSimpleNameExpression : PsiElement {
    fun getReferencedNameElement(): PsiElement
}
class JetOperationReferenceExpression : JetSimpleNameExpression {
    override fun getReferencedNameElement() = this
}
class JetLabelReferenceExpression : JetSimpleNameExpression {
    public override fun getReferencedNameElement(): PsiElement =
            findChildByType(42) ?: this
}

fun box(): String {
    val element = JetLabelReferenceExpression().getReferencedNameElement()
    return if (element is JetOperationReferenceExpression) "OK" else "fail"
}
