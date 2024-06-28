fun box(): String {
    if (true) X::<!INVISIBLE_REFERENCE!>y<!> else null
    return "OK"
}

object X {
    private val y = null
}