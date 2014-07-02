import kotlin.reflect.*

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
    
    fun main() {
        val x = ::foo
        val y = ::bar
        val z = ::baz

        x : KMemberFunction0<A, Unit>
        y : KMemberFunction1<A, Int, Unit>
        z : KMemberFunction0<A, String>
    }
}
