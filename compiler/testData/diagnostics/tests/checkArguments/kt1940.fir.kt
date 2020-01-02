//KT-1940 Exception while repeating named parameters
package kt1940

fun foo(i: Int) {}

fun test() {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(1, i = 2) //exception
    <!INAPPLICABLE_CANDIDATE!>foo<!>(i = 1, i = 2) //exception
}