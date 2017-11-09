// "Replace with 'NewClass'" "false"
// ACTION: Convert to block body
// ACTION: Introduce local variable


@Deprecated("", replaceWith = ReplaceWith("NewClass"))
class OldClass()

typealias Old1 = OldClass
typealias Old2 = Old1

class NewClass()

fun foo() = <caret>Old2()