// "Replace with 'NewClass<Int>'" "true"
package ppp

@Deprecated("", ReplaceWith("NewClass<Int>"))
class OldClass<T, V>

class NewClass<F>

fun foo() {
    <caret>OldClass<Int, String>()
}
