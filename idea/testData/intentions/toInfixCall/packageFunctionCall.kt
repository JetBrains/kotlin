// IS_APPLICABLE: false
// WARNING: 'infix' modifier is inapplicable on this function
package ppp

infix fun foo(p: String){}

fun main() {
    ppp.<caret>foo("")
}