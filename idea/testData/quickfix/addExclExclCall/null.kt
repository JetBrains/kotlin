// "Add non-null asserted (!!) call" "false"
// ACTION: Convert to lazy property
// ACTION: Add 'const' modifier
// ACTION: Add 'toString()' call
// ACTION: Change type of 'x' to 'String?'
// ACTION: Convert property initializer to getter
// ERROR: Null can not be a value of a non-null type String

val x: String = null<caret>