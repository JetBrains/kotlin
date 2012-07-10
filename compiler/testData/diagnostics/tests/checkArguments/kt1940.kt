//KT-1940 Exception while repeating named parameters
package kt1940

fun foo(<!UNUSED_PARAMETER!>i<!>: Int) {}

fun test() {
    foo(1, <!ARGUMENT_PASSED_TWICE, MIXING_NAMED_AND_POSITIONED_ARGUMENTS!>i<!> = 2) //exception
    foo(i = 1, <!ARGUMENT_PASSED_TWICE!>i<!> = 2) //exception
}