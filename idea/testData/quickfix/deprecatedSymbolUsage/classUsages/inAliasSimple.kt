// "Replace with 'NewClass'" "true"

@Deprecated("", ReplaceWith("NewClass"))
class OldClass

class NewClass

typealias Old = <caret>OldClass

val a: Old = Old()