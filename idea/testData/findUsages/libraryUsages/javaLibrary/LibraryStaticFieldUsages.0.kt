// PSI_ELEMENT: com.intellij.psi.PsiField
// OPTIONS: usages
// FIND_BY_REF
// FIR_IGNORE

package usages

import library.Foo

fun test() {
    Foo.<caret>X = 1
}