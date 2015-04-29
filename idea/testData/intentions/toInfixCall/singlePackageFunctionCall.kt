// WITH_RUNTIME
// IS_APPLICABLE: false
package demo

fun foo(str: String) = kotlin.io.println(str)

fun main() {
    <caret>demo.foo("")
}