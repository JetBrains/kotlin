// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
fun test1() {
    try {

    } catch (<!SYNTAX!><!>)<!SYNTAX!><!>
}

fun test2() {
    try { }<!SYNTAX!><!>
}

fun test3() {
    try {
    } catch (<!SYNTAX!><!>{}<!SYNTAX!>)<!> {}
}
