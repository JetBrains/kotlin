// "Replace with 'NewClass<Int>'" "true"
package ppp

@Deprecated("renamed", ReplaceWith("NewClass<T>"))
class OldClass<T>

class NewClass<F>

fun foo() {
    <caret>OldClass<Int>()
}
