//ALLOW_AST_ACCESS
package test

import kotlin.reflect.KProperty

class A {
    var a by MyProperty()
}

class MyProperty<T> {
    fun getValue(t: T, p: KProperty<*>): Int = 42
    fun setValue(t: T, p: KProperty<*>, i: Int) {}
}
