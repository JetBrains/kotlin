// ISSUE: KT-44932
// WITH_STDLIB

abstract class PsiElement {
    abstract val parent: PsiElement
}

class KtNameReferenceExpression(override val parent: PsiElement) : PsiElement()

class OtherElement(override val parent: PsiElement) : PsiElement()

class KtDotQualifiedExpression : PsiElement() {
    override val parent: PsiElement
        get() = this

    val psi: PsiElement = EndElement()
}

class EndElement : PsiElement() {
    override val parent: PsiElement
        get() = this
}

fun mark(element: PsiElement): String {
    when (element) {
        is KtNameReferenceExpression -> {
            var parent = element
            repeat(2) {
                parent = parent.parent
                (parent as? KtDotQualifiedExpression)?.psi?.let { return mark(it) }
            }
        }
    }
    return if (element is EndElement) "OK" else "Fail"
}

fun box(): String {
    val element = KtNameReferenceExpression(OtherElement(KtDotQualifiedExpression()))
    return mark(element)
}
