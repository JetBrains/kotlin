// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIND_BY_REF
// FIR_COMPARISON

package usages

import library.Foo

fun test() {
    <caret>Foo(1)
}