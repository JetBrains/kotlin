fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}
fun bar(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>b<!>: Any) {}

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
        arr[1] <!UNREACHABLE_CODE!>=<!> break
    }

    while (true) {
        break
        <!UNREACHABLE_CODE!>foo(1)<!>
    }

    while (true) {
        var <!UNUSED_VARIABLE!>x<!> = 1
        break
        <!UNREACHABLE_CODE!>x = 2<!>
    }

    while (true) {
        var <!UNUSED_VARIABLE!>x<!> = 1
        <!UNREACHABLE_CODE!>x =<!> break
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
