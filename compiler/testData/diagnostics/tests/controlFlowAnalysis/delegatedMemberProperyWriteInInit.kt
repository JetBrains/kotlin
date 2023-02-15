// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.reflect.KProperty

interface Delegate<V> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): V = null!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {}
}

fun <V> delegate(): Delegate<V> = null!!
fun consume(x: Any?) {}

class A {
    init {
        consume(<!UNINITIALIZED_VARIABLE!>x<!>)
        <!VAL_REASSIGNMENT!>x<!> = 10
    }

    val x: Int by delegate()

    init {
        x = 10
        consume(x)
    }
}

class B {
    init {
        consume(<!UNINITIALIZED_VARIABLE!>x<!>)
        <!INITIALIZATION_BEFORE_DECLARATION!>x<!> = 10
    }

    var x: Int by delegate()

    init {
        x = 10
        consume(x)
    }
}
