// PSI_ELEMENT: com.intellij.psi.PsiMethod
// OPTIONS: usages
// FIND_BY_REF
// FIR_IGNORE

package usages

import library.Foo

fun test() {
    Foo(1).<caret>bar(1)
}