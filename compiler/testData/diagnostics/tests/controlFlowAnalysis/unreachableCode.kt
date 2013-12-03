fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {}
fun bar(<!UNUSED_PARAMETER!>a<!>: Any, <!UNUSED_PARAMETER!>b<!>: Any) {}

fun test(arr: Array<Int>) {
    while (true) {
        <!UNREACHABLE_CODE!>foo<!>(break)
    }


    while (true) {
        <!UNREACHABLE_CODE!>bar<!>(arr, break)
    }

    while (true) {
        <!UNREACHABLE_CODE!>arr[break]<!>
    }

    while (true) {
        <!UNREACHABLE_CODE!>arr[1]<!> = break
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
        <!UNREACHABLE_CODE!>x = break<!>
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
        <!USELESS_ELVIS!>break<!> ?: <!UNREACHABLE_CODE!>null<!>
    }
}
