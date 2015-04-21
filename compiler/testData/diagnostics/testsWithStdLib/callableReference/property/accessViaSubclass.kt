// !CHECK_TYPE

import kotlin.reflect.KMemberProperty

open class Base {
    val foo: Int = 42
}

open class Derived : Base()

fun test() {
    val o = Base::foo
    checkSubtype<KMemberProperty<Base, Int>>(o)
    checkSubtype<Int>(o.get(Derived()))
}
