//KT-1955 Half a file is red on incomplete code

package b

fun foo() {
    val <!UNUSED_VARIABLE!>a<!> = 1<!SYNTAX!><!>


