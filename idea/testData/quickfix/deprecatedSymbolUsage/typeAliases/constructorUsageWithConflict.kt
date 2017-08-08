// "Replace with 'New'" "false"
// ACTION: Convert to block body
// ACTION: Introduce local variable
// ACTION: Replace usages of '<init>(): Old /* = OldClass */' in whole project
// ACTION: Replace with 'NewClass(12)'

@Deprecated("Use NewClass", replaceWith = ReplaceWith("NewClass"))
class OldClass @Deprecated("Use NewClass(12)", replaceWith = ReplaceWith("NewClass(12)")) constructor()

@Deprecated("Use New", replaceWith = ReplaceWith("New"))
typealias Old = OldClass

class NewClass(p: Int = 12)
typealias New = NewClass

// 'New' replacement shouldn't be used because it may brake code behaviour
fun foo() = <caret>Old()