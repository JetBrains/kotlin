// WITH_RUNTIME
// IS_APPLICABLE: false
// WARNING: 'infix' modifier is inapplicable on this function

package demo

infix fun foo(str: String) = kotlin.io.println(str)

fun main() {
    <caret>demo.foo("")
}