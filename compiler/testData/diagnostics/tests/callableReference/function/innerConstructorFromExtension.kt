// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction1

class A {
    inner class Inner
}
    
fun A.main() {
    val x = ::Inner
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(x)
    checkSubtype<KFunction1<A, A.Inner>>(y)
}

fun Int.main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    checkSubtype<KFunction1<A, A.Inner>>(y)
}
