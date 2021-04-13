// PSI_ELEMENT: com.intellij.psi.PsiField
// OPTIONS: usages
// FIND_BY_REF

package usages

import library.Foo

fun test() {
    Foo.<caret>X = 1
}