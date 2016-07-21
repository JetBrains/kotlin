// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function: must be a member or an extension function
package ppp

infix fun foo(p: String){}

fun main() {
    ppp.<caret>foo("")
}
