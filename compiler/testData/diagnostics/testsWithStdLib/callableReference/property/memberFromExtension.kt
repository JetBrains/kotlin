// !CHECK_TYPE

import kotlin.reflect.*

class A {
    val foo: Unit = Unit
    var bar: String = ""
    var self: A
        get() = this
        set(value) { }
}
    
fun A.test() {
    val x = ::foo
    val y = ::bar
    val z = ::self

    checkSubtype<KMemberProperty<A, Unit>>(x)
    checkSubtype<KMutableMemberProperty<A, String>>(y)
    checkSubtype<KMutableMemberProperty<A, A>>(z)

    y.set(z.get(A()), x.get(A()).toString())
}
