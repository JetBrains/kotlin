// NO_ERRORS_DUMP
package a

import kotlin.reflect.KProperty

interface T

fun T.getValue(thisRef: B, desc: KProperty<*>): Int {
    return 3
}

fun T.setValue(thisRef: B, desc: KProperty<*>, value: Int) {
}

class A(): T

<selection>class B {
    var v by A()
}</selection>
