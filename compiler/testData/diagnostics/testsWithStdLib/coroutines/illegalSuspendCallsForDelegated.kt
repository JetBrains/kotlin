fun bar(d: Delegate): String {
    val x: String by <!ILLEGAL_SUSPEND_FUNCTION_CALL!>d<!>
    return x
}

class Delegate {
    suspend <!UNSUPPORTED!>operator<!> fun getValue(thisRef: Any?, property: Any?): String = ""
}
