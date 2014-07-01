import kotlin.reflect.KMemberFunction0

class A {
    inner class Inner
}
    
fun A.main() {
    val x = ::Inner
    val y = A::Inner

    x : KMemberFunction0<A, A.Inner>
    y : KMemberFunction0<A, A.Inner>
}

fun Int.main() {
    ::<!UNRESOLVED_REFERENCE!>Inner<!>
    val y = A::Inner

    y : KMemberFunction0<A, A.Inner>
}
