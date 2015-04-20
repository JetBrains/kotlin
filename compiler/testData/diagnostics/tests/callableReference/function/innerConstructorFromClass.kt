// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KMemberFunction0

class A {
    inner class Inner
    
    fun main() {
        val x = ::Inner
        val y = A::Inner

        checkSubtype<KMemberFunction0<A, A.Inner>>(x)
        checkSubtype<KMemberFunction0<A, Inner>>(y)
    }
    
    companion object {
        fun main() {
            ::<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>Inner<!>
            val y = A::Inner

            checkSubtype<KMemberFunction0<A, A.Inner>>(y)
        }
    }
}

class B {
    fun main() {
        ::<!UNRESOLVED_REFERENCE!>Inner<!>
        val y = A::Inner

        checkSubtype<KMemberFunction0<A, A.Inner>>(y)
    }
}