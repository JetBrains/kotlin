// ISSUE: KT-67808

open class Base<T> {
    val x: Any?
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val y: T<!>

    init {
        <!DEBUG_INFO_LEAKING_THIS!>this<!> as Derived
        x = "O"
        <!VAL_REASSIGNMENT!><!DEBUG_INFO_SMARTCAST!>this<!>.y<!> = "O"
    }
}

class Derived: Base<String>()
