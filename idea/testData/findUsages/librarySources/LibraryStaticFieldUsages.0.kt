// PSI_ELEMENT: com.intellij.psi.PsiField
// OPTIONS: usages
// FIND_BY_REF
// FIND_BY_NAVIGATION_ELEMENT
package usages

import library.Foo

fun test() {
    Foo.<caret>X = 1
}