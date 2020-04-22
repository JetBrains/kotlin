// FILE: annotation.kt

package kotlin.native.concurrent

annotation class ThreadLocal

// FILE: test.kt

import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

var global = 0

object ImmutableSingleton {
    val immutableField = 0
    val property: Int
        get() {
            return immutableField
        }
}

@ThreadLocal
object MutableSingletonMarkedWithThreadLocal {
    var mutableField = 0
}

object MutableSingletonWithoutThreadLocal {
    <!MUTABLE_SINGLETON!>var mutableField = 0<!>
}

object SinletonWithMutableFieldImplementedByDelegate {
    var delegatedField: Int by Delegate()
}

object SingletonWithSetter {
    var fieldWithSetter = 0
        set(value) {
            global = value
        }
}

class NestedSingletonWithoutThreadLocal {
    companion object {
        <!MUTABLE_SINGLETON!>var mutableField = 0<!>
    }
}

class ImmutableNestedSingleton {
    companion object {
        val immutableField = 0
        val property: Int
            get() {
               return immutableField
            }
        }
}


class NestedSingletonWithThreadLocal {
    @ThreadLocal
    companion object {
        var mutableField = 0
    }
}

class NestedSinletonWithMutableFieldImplementedByDelegate {
    companion object {
        var delegatedField: Int by Delegate()
    }
}

class NestedSingletonWithSetter {
    companion object {
        var fieldWithSetter = 0
            set(value) {
                global = value
            }
    }
}
