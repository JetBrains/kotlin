// See also KT-10869: Accessing lazy properties from init causes IllegalArgumentException

import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class Kaboom() {
    // Here and below we should have errors for simple AND delegated
    init {
        <!UNINITIALIZED_VARIABLE, DEBUG_INFO_LEAKING_THIS!>delegated<!>.hashCode()
        <!UNINITIALIZED_VARIABLE!>simple<!>.hashCode()
        <!DEBUG_INFO_LEAKING_THIS!>withGetter<!>.hashCode()
    }

    val other = <!UNINITIALIZED_VARIABLE, DEBUG_INFO_LEAKING_THIS!>delegated<!>

    val another = <!UNINITIALIZED_VARIABLE!>simple<!>

    val something = <!DEBUG_INFO_LEAKING_THIS!>withGetter<!>
    
    val delegated: String by CustomDelegate()

    val simple = "xyz"

    val withGetter: String
        get() = "abc"

    // No error should be here
    val after = <!DEBUG_INFO_LEAKING_THIS!>delegated<!>
}
