data class StringPair(val first: String, val second: String)

infix fun String.to(second: String) = StringPair(this, second)

fun f(a: String?) {
    if (a != null) {
        val <!UNUSED_VARIABLE!>b<!>: StringPair = <!DEBUG_INFO_SMARTCAST!>a<!> to <!DEBUG_INFO_SMARTCAST!>a<!>
    }
}