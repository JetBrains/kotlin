// "Replace with 'New<T, U>'" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification

@Deprecated("Use New", replaceWith = ReplaceWith("New<T, U>"))
class Old<T, U>

@Deprecated("Use New1", replaceWith = ReplaceWith("New1"))
class Old1

@Deprecated("Use New2", replaceWith = ReplaceWith("New2"))
class Old2

typealias OOO = Old<Old1, Old2>

class New<T, U>
class New1
class New2

fun foo(): <caret>OOO? = null