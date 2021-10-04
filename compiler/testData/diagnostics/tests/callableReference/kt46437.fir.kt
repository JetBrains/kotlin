fun box(): String {
    if (true) X::<!UNRESOLVED_REFERENCE!>y<!> else null
    return "OK"
}

object X {
    private val y = null
}