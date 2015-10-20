// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtPrimaryConstructor
// OPTIONS: usages
// FIND_BY_REF
// WITH_FILE_NAME
package usages

import library.*

class X: A.T {
    constructor(n: Int): super(n)
}

class Y(): A.T(1)

fun test() {
    val a: A.T = A.T()
    val aa = A.<caret>T(1)
}

