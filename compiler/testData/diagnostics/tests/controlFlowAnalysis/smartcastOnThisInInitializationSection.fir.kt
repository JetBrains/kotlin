// ISSUE: KT-67808

open class Base<T> {
    val x: Any?
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>val y: T<!>

    init {
        this as Derived
        x = "O"
        this.<!VAL_REASSIGNMENT!>y<!> = <!ASSIGNMENT_TYPE_MISMATCH!>"O"<!>
    }
}

class Derived: Base<String>()
