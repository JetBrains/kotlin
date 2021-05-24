// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_VARIABLE -UNUSED_PARAMETER -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE

fun <T : CharSequence> bar1(x: T) {}
fun bar2(x: CharSequence) {}
fun bar3(x: String) {}

fun <T : CharSequence?> foo(x: T) {
    var y1: CharSequence = ""
    var y2: String = ""
    if (x != null) {
        if (x != null) {}

        y1 = x
        y2 = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>

        bar1(x)
        bar1<CharSequence>(x)
        bar2(x)
        bar3(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    }

    if (x is String) {
        y1 = x
        y2 = x

        bar1(x)
        bar2(x)
        bar3(x)
    }

    if (x is CharSequence) {
        y1 = x
        y2 = <!ASSIGNMENT_TYPE_MISMATCH!>x<!>

        bar1(x)
        bar2(x)
        bar3(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
    }

    if (1 == 1) {
        val y = x!!
        bar1(x)
        bar1<CharSequence>(x)
        bar2(x)
        bar3(<!ARGUMENT_TYPE_MISMATCH!>x<!>)

        bar1(y)
        bar2(y)
        bar3(<!ARGUMENT_TYPE_MISMATCH!>y<!>)
    }
}
