// PSI_ELEMENT: com.intellij.psi.PsiClass
// OPTIONS: usages
// FIND_BY_REF
// FIR_COMPARISON

package usages

import library.Foo

fun test() {
    val foo: <caret>Foo
}