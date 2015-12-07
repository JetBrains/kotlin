// "Create local variable 'foo'" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

val t: Int = <caret>foo