// PSI_ELEMENT: com.intellij.psi.PsiField
// OPTIONS: usages
// FIND_BY_REF
// FIND_BY_MIRROR_ELEMENT
import java.awt.Dimension

fun test() {
    Dimension().<caret>width = 1
}