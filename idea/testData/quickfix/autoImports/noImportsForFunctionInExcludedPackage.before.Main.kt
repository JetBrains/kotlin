// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Convert to lazy property
// ACTION: Convert property initializer to getter
// ACTION: Create function 'someFunction'
// ACTION: Rename reference
// ERROR: Unresolved reference: someFunction

val x = <caret>someFunction()