fun bar() {
    false and false
}

// See exception in KT-13421
fun foo() {
    42 <!INAPPLICABLE_CANDIDATE!>and<!> false
}
