// IGNORE_REVERSED_RESOLVE
// ISSUE: KT-10869, KT-56682

import kotlin.reflect.KProperty

class CustomDelegate {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = prop.name
}

class Kaboom() {
    // Here and below we should have errors for simple AND delegated
    init {
        <!DEBUG_INFO_LEAKING_THIS, UNINITIALIZED_VARIABLE!>delegated<!>.hashCode()
        <!UNINITIALIZED_VARIABLE!>simple<!>.hashCode()
        <!DEBUG_INFO_LEAKING_THIS!>withGetter<!>.hashCode()
    }

    val other = <!DEBUG_INFO_LEAKING_THIS, UNINITIALIZED_VARIABLE!>delegated<!>

    val another = <!UNINITIALIZED_VARIABLE!>simple<!>

    val something = <!DEBUG_INFO_LEAKING_THIS!>withGetter<!>
    
    val delegated: String by CustomDelegate()

    val simple = "xyz"

    val withGetter: String
        get() = "abc"

    // No error should be here
    val after = <!DEBUG_INFO_LEAKING_THIS!>delegated<!>
}
