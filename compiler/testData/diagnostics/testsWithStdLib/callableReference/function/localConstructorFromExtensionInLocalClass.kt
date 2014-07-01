import kotlin.reflect.KFunction0

fun main() {
    class A
    
    class B {
        fun Int.foo() {
            val x = ::A
            x : KFunction0<A>
        }
        fun A.foo() {
            val x = ::A
            x : KFunction0<A>
        }
    }
}
