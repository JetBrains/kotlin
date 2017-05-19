fun bar(arg: Any?) = arg

fun foo() {
    var s: String?
    s = null
    try {
        s = "Test"
    } catch (ex: Exception) {}
    bar(<!DEBUG_INFO_CONSTANT!>s<!>)
    if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>s<!> != null<!>) { }
    <!DEBUG_INFO_CONSTANT!>s<!><!UNSAFE_CALL!>.<!>hashCode()
}