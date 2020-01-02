// !CHECK_TYPE

import kotlin.reflect.KProperty1

open class Base {
    val foo: Int = 42
}

open class Derived : Base()

fun test() {
    val o = Base::foo
    checkSubtype<KProperty1<Base, Int>>(o)
    checkSubtype<Int>(o.get(Derived()))
}
