// !DIAGNOSTICS: -UNUSED_EXPRESSION
import kotlin.reflect.KFunction0

class A {
    class Nested
    
    fun main() {
        val x = ::Nested
        val y = A::Nested

        x : KFunction0<Nested>
        y : KFunction0<Nested>
    }
    
    default object {
        fun main() {
            ::Nested
            val y = A::Nested

            y : KFunction0<A.Nested>
        }
    }
}

class B {
    fun main() {
        ::<!UNRESOLVED_REFERENCE!>Nested<!>
        val y = A::Nested

        y : KFunction0<A.Nested>
    }
}