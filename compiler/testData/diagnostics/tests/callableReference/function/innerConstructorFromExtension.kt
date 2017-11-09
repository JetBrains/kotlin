// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !LANGUAGE: +CallableReferencesToClassMembersWithEmptyLHS

import kotlin.reflect.KFunction1

class A {
    inner class Inner
}
    
fun A.main() {
    ::Inner
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}

fun Int.main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}
