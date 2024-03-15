fun foo(a: Any) {}
fun bar(a: Any, b: Any) {}

fun test(arr: Array<Int>) {
    while (true) {
        <!UNREACHABLE_CODE!>foo(<!>break<!UNREACHABLE_CODE!>)<!>
    }


    while (true) {
        <!UNREACHABLE_CODE!>bar(<!>arr, break<!UNREACHABLE_CODE!>)<!>
    }

    while (true) {
        arr<!UNREACHABLE_CODE!>[<!>break<!UNREACHABLE_CODE!>]<!>
    }

    while (true) {
        arr<!UNREACHABLE_CODE!>[<!>1<!UNREACHABLE_CODE!>] =<!> break
    }

    while (true) {
        break
        <!UNREACHABLE_CODE!>foo(1)<!>
    }

    while (true) {
        var <!VARIABLE_NEVER_READ!>x<!> = 1
        break
        <!UNREACHABLE_CODE!><!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> = 2<!>
    }

    while (true) {
        var <!VARIABLE_NEVER_READ!>x<!> = 1
        <!UNREACHABLE_CODE!><!ASSIGNED_VALUE_IS_NEVER_READ!>x<!> =<!> break
    }

    // TODO: bug, should be fixed in CFA
    while (true) {
        if (1 > 2 && break && 2 > 3) {

        }
    }

    // TODO: bug, should be fixed in CFA
    while (true) {
        if (1 > 2 || break || 2 > 3) {

        }
    }

    while (true) {
        break <!UNREACHABLE_CODE, USELESS_ELVIS!>?: null<!>
    }
}
