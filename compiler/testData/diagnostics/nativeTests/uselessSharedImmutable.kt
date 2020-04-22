// FILE: annotation.kt

package kotlin.native.concurrent

@Target(AnnotationTarget.PROPERTY)
annotation class SharedImmutable

// FILE: test.kt

import kotlin.native.concurrent.SharedImmutable
import kotlin.reflect.KProperty

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = 0

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

data class Box(val value: Int = 0)

var global = Box()

@SharedImmutable
val valWithSharedImmutable = Box()

<!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
var varWithSharedImmutable = Box()

@SharedImmutable
var varDelegated: Int by Delegate()

<!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
var property = Box()
    get() {
        return field
    }
    set(value) {
        global = value
    }

class NotGlobalSharedImmutable {
    <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
    var mutableField = 0
    <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
    val field = 0
    <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
    val property1: Box
        get() = valWithSharedImmutable
    <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
    var property2: Int by Delegate()
}

data class NotGlobalSharedImmutable2(<!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!> var mutableField: Int, <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!> val field: Int)

class NotGlobalSharedImmutable3 {
    companion object {
        <!USELESS_SHARED_IMMUTABLE!>@SharedImmutable<!>
        val field = 0
    }
}
