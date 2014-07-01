import kotlin.reflect.*

class A(var g: A) {
    val f: Int = 0

    fun test() {
        ::f : KMemberProperty<A, Int>
        ::g : KMutableMemberProperty<A, A>
    }
}
