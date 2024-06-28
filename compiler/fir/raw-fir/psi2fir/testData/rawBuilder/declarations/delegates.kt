// IGNORE_TREE_ACCESS: KT-64898
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

val x: Int by lazy { 1 + 2 }

val delegate = object: ReadWriteProperty<Any?, Int> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Int = 1
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {}
}

val value by delegate

var variable by delegate

interface Base {
}

class Derived(b: Base) : Base by b {
}
