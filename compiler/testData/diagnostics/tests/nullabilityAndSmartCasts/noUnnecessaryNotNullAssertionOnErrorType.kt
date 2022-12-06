// FIR_IDENTICAL
package a

fun foo() {
    bar()!!
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>