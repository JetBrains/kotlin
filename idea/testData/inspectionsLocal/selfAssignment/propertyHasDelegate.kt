// PROBLEM: none
// WITH_RUNTIME

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Test {
    var foo: Int by Delegate()

    fun test() {
        foo = <caret>foo
    }
}

class Delegate : ReadWriteProperty<Test, Int> {
    override fun getValue(thisRef: Test, property: KProperty<*>): Int = 1
    override fun setValue(thisRef: Test, property: KProperty<*>, value: Int) {
        println()
    }
}