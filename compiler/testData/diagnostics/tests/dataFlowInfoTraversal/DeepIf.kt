// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// !WITH_NEW_INFERENCE
fun bar(x: Int) = x + 1

fun foo() {
    val x: Int? = null

    if (x != null) {
        bar(x)
        if (<!SENSELESS_COMPARISON!>x != null<!>) {
            bar(x)
            if (1 < 2) bar(x)
            if (1 > 2) bar(x)
        }
        if (<!SENSELESS_COMPARISON!>x == null<!>) {
            <!NI;UNREACHABLE_CODE!>bar(<!>x<!NI;UNREACHABLE_CODE!>)<!>
        }
        if (<!SENSELESS_COMPARISON!>x == null<!>) <!NI;UNREACHABLE_CODE!>bar(<!>x<!NI;UNREACHABLE_CODE!>)<!> else bar(x)
        bar(bar(x))
    } else if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>) {
        bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>x<!>)
        if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> != null<!>) {
            <!NI;UNREACHABLE_CODE!>bar(<!>x<!NI;UNREACHABLE_CODE!>)<!>
            <!NI;UNREACHABLE_CODE!>if (<!SENSELESS_COMPARISON!>x == null<!>) bar(x)<!>
            <!NI;UNREACHABLE_CODE!>if (<!SENSELESS_COMPARISON!>x == null<!>) bar(x) else bar(x)<!>
            <!NI;UNREACHABLE_CODE!>bar(bar(x) + bar(x))<!>
        } else if (<!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>) {
            bar(<!DEBUG_INFO_CONSTANT, TYPE_MISMATCH!>x<!>)
        }
    }

}
