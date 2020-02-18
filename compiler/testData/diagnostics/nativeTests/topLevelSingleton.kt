// FILE: annotation.kt
package kotlin.native.concurrent

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ThreadLocal

// FILE: test.kt
import kotlin.native.concurrent.ThreadLocal

import kotlin.reflect.KProperty

class Delegate {
    val value: Int = 10
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

class AtomicInt(var value: Int)
object Foo {
    <!VARIABLE_IN_TOP_LEVEL_SINGLETON_WITHOUT_THERAD_LOCAL!>var field1: Int = 10<!>
    val backer2 = AtomicInt(0)
    var field2: Int
        get() = backer2.value
        set(value: Int) { backer2.value = value }
}

object Foo1 {
    var field1: Int = 10
        set(value: Int) { backer2.value = value }
    val backer2 = AtomicInt(0)
}

object WithDelegate {
    var field1: Int by Delegate()
}

@ThreadLocal
object Bar {
    var field1: Int = 10
    var field2: String? = null
}

var topLevelProperty = "Global var"