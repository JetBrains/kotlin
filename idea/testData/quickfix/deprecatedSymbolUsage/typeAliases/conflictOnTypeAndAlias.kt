// "Replace with 'NewClass'" "false"
// ACTION: Introduce local variable
// ACTION: Replace usages of 'typealias Old = OldClass' in whole project
// ACTION: Replace with 'New'


@Deprecated("Use NewClass", ReplaceWith("NewClass"))
class OldClass

@Deprecated("Use New", ReplaceWith("New"))
typealias Old = OldClass

class NewClass
typealias New = NewClass

fun foo() {
    <caret>Old()
}
