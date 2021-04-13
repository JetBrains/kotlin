// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIND_BY_REF

package usages

import library.Foo

fun test() {
    Foo.<caret>baz(1)
}