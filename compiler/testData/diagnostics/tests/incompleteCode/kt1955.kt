// FIR_IDENTICAL
//KT-1955 Half a file is red on incomplete code

package b

fun foo() {
    val a = 1<!SYNTAX!><!>
