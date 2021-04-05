fun bar() {
    false and false
}

// See exception in KT-13421
fun foo() {
    42 and <!ARGUMENT_TYPE_MISMATCH!>false<!>
}
