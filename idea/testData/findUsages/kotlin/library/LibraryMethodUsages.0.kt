// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIND_BY_REF
// FIND_BY_MIRROR_ELEMENT
import java.awt.Dimension

fun test() {
    Dimension().<caret>setSize(1.0, 2.0)
}