// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "false"
// ACTION: Remove unnecessary non-null assertion (!!)

open class Base(s: String)
class Child: Base(""<caret>!!)