<!DIRECTIVES("HELPERS: REFLECT")!>

package org.jetbrains.<!ELEMENT(1)!>

open class A {
    companion object <!ELEMENT(2)!> {

    }
}

class B {
    companion object <!ELEMENT(3)!>: A() {

    }
}

fun box(): String? {
    if (!checkCompanionObjectName(A::class, "org.jetbrains.<!ELEMENT_VALIDATION(1)!>.A.<!ELEMENT_VALIDATION(2)!>")) return null
    if (!checkCompanionObjectName(B::class, "org.jetbrains.<!ELEMENT_VALIDATION(1)!>.B.<!ELEMENT_VALIDATION(3)!>")) return null

    return "OK"
}
