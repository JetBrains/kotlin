// "Replace with 'NewClass(12)'" "true"

@Deprecated("Use NewClass", replaceWith = ReplaceWith("NewClass"))
class OldClass @Deprecated("Use NewClass(12)", level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("NewClass(12)")) constructor()

@Deprecated("Use New", replaceWith = ReplaceWith("New"))
typealias Old = OldClass

class NewClass(p: Int = 12)
typealias New = NewClass

fun foo() = <caret>Old()