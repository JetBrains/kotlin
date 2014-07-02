import kotlin.reflect.KMemberFunction0

class A {
    inner class Inner
}

fun main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    y : KMemberFunction0<A, A.Inner>
}
