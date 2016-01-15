// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function
package ppp

infix fun foo(p: String){}

fun main() {
    ppp.<caret>foo("")
}