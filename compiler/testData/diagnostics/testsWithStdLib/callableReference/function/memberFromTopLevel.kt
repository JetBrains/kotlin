import kotlin.reflect.*

class A {
    fun foo() {}
    fun bar(<!UNUSED_PARAMETER!>x<!>: Int) {}
    fun baz() = "OK"
}

fun main() {
    val x = A::foo
    val y = A::bar
    val z = A::baz

    x : KMemberFunction0<A, Unit>
    y : KMemberFunction1<A, Int, Unit>
    z : KMemberFunction0<A, String>
}
