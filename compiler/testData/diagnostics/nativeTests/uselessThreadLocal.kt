// FILE: annotation.kt
package kotlin.native.concurrent

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class ThreadLocal

// FILE: test.kt
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

data class Box(val value: Int = 0)

var global = Box()

@ThreadLocal
val anonymousSingletonWithThreadLocal = object {
    val x = 0
}

@ThreadLocal
val valWithThreadLocal = Box()

<!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
var varWithThreadLocal = Box()

@ThreadLocal
var varDelegated: Int by Delegate()

<!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
var property = Box()
    get() {
        return field
    }
    set(value) {
        global = value
    }

class NotGlobalThreadLocal {
    <!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
    var mutableField = 0
    <!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
    val field = 0
    <!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
    val property1: Box
        get() = valWithThreadLocal
    <!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
    var property2: Int by Delegate()
}

data class NotGlobalThreadLocal2(<!USELESS_THREAD_LOCAL!>@ThreadLocal<!> var mutableField: Int, <!USELESS_THREAD_LOCAL!>@ThreadLocal<!> val field: Int)

class NotGlobalThreadLocal3 {
    @ThreadLocal
    companion object {
        <!USELESS_THREAD_LOCAL!>@ThreadLocal<!>
        val field = 0
    }
}

@ThreadLocal
object SingletonWithThreadLocal {
    var mutableField = 0
}