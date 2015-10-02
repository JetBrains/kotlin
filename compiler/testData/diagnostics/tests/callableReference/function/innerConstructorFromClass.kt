// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction1

class A {
    inner class Inner
    
    fun main() {
        ::<!CALLABLE_REFERENCE_TO_MEMBER_OR_EXTENSION_WITH_EMPTY_LHS!>Inner<!>
        val y = A::Inner

        checkSubtype<KFunction1<A, Inner>>(y)
    }
    
    companion object {
        fun main() {
            ::<!UNRESOLVED_REFERENCE!>Inner<!>
            val y = A::Inner

            checkSubtype<KFunction1<A, A.Inner>>(y)
        }
    }
}

class B {
    fun main() {
        ::<!UNRESOLVED_REFERENCE!>Inner<!>
        val y = A::Inner

        checkSubtype<KFunction1<A, A.Inner>>(y)
    }
}