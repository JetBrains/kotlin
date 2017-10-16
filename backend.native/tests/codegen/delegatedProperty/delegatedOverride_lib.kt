package a

import kotlin.reflect.KProperty

open class A {
    open val x = 42
}

class Delegate {
    val f = 117
    operator fun getValue(receiver: Any?, p: KProperty<*>): Int {
        println(p.name)
        return f
    }
}

open class B: A() {
    override val x: Int by Delegate()

    fun bar() {
        println(super<A>.x)
    }
}