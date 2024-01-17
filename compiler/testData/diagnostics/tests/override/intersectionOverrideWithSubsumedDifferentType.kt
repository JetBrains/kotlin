// FIR_IDENTICAL

interface UAnnotationEx : UAnnotation, UAnchorOwner
interface UAnchorOwner : UElement
interface UElement {
    val psi: PsiElement?

    val javaPsi: PsiElement?
        get() = psi
}
interface UAnnotation : UElement {
    override val javaPsi: PsiAnnotation?
}

interface PsiElement
interface PsiAnnotation : PsiElement