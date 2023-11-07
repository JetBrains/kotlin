// FIR_IDENTICAL
// ISSUE: KT-57529

fun <<!SYNTAX!>break<!>> foo() {}

fun test(){
    foo<String>()
}
