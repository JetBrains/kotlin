// "Replace with 'NewClass'" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification


@Deprecated("", ReplaceWith("NewClass"))
class OldClass
typealias Old = OldClass

class NewClass

fun foo(): <caret>Old = null!!