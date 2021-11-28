// FIR_IDENTICAL
//KT-1940 Exception while repeating named parameters
package kt1940

fun foo(i: Int) {}

fun test() {
    foo(1, <!ARGUMENT_PASSED_TWICE!>i<!> = 2) //exception
    foo(i = 1, <!ARGUMENT_PASSED_TWICE!>i<!> = 2) //exception
}
