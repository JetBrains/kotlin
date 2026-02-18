// WITH_REFLECT
// FILE: lib.kt
package dependency

import kotlin.reflect.KProperty

class Holder(val data: Any)

interface MyDelegateOperators {
    operator fun Holder.getValue(thisRef: Any?, property: KProperty<*>): String = data as String
    operator fun Holder.setValue(thisRef: Any?, property: KProperty<*>, value: String) {}
}

// FILE: main.kt
package test

import dependency.*
import test.MyObject.getValue
import test.MyObject.setValue

object MyObject : MyDelegateOperators

var prop: String <expr>by Holder("hello")</expr>
