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

    checkSubtype<KProperty1<A, Unit>>(x)
    checkSubtype<KMutableProperty1<A, String>>(y)
    checkSubtype<KMutableProperty1<A, A>>(z)

    y.set(z.get(A()), x.get(A()).toString())
}
