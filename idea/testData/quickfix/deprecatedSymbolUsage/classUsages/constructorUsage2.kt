// "Replace with 'java.util.Random'" "true"

@deprecated("", ReplaceWith("java.util.Random"))
class OldClass

fun foo() {
    <caret>OldClass(1)
}
