class A {
    inner class Inner
    
    fun main() {
        val x = ::Inner
        val y = A::Inner

        x : KMemberFunction0<A, A.Inner>
        y : KMemberFunction0<A, Inner>
    }
    
    class object {
        fun main() {
            ::<!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>Inner<!>
            val y = A::Inner

            y : KMemberFunction0<A, A.Inner>
        }
    }
}

class B {
    fun main() {
        ::<!UNRESOLVED_REFERENCE!>Inner<!>
        val y = A::Inner

        y : KMemberFunction0<A, A.Inner>
    }
}
