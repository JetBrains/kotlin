// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Convert to lazy property
// ACTION: Convert property initializer to getter
// ACTION: Create class 'SomeClass'
// ACTION: Create function 'SomeClass'
// ACTION: Rename reference
// ERROR: Unresolved reference: SomeClass

val x = <caret>SomeClass()