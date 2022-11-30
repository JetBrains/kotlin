fun box(): String {
    <!INAPPLICABLE_CANDIDATE!>if (true) X::<!UNRESOLVED_REFERENCE!>y<!> else null<!>
    return "OK"
}

object X {
    private val y = null
}
