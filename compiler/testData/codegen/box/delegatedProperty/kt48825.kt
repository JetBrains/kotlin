// WITH_RUNTIME

import kotlin.reflect.KProperty

var result = "Fail"

open class A {
    protected inline operator fun <reified V : Any> A.setValue(thisRef: Any?, property: KProperty<*>, value: V?): V? {
        result = property.name
        return value
    }

    protected inline operator fun <reified V : Any> A.getValue(thisRef: Any?, property: KProperty<*>): V = 0 as V
}

class B : A() {
    var OK: Int by this
}

fun box(): String {
    val b = B()
    b.OK = 1
    return result
}
