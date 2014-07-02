import kotlin.reflect.KMemberFunction0

class A {
    fun foo() = 42
}

fun A.foo() {}

fun main() {
    val x = A::foo

    x : KMemberFunction0<A, Int>
}
