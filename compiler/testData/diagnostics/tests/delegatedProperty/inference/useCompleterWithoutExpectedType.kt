// !CHECK_TYPE

import kotlin.reflect.KProperty

class A {
    val a by MyProperty()

    fun test() {
        checkSubtype<Int>(a)
    }
}

class MyProperty<R> {
    operator fun getValue(thisRef: R, desc: KProperty<*>): Int = throw Exception("$thisRef $desc")
}
