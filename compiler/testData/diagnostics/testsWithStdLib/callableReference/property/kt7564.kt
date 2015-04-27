import kotlin.reflect.*

fun <T> checkSubtype(t: T) = t

class A(var g: A) {
    val f: Int = 0

    fun test() {
        checkSubtype<KMemberProperty<A, Int>>(::f)
        checkSubtype<KMutableMemberProperty<A, A>>(::g)
    }
}