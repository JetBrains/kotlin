// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create class 'SomeClass'
// ACTION: Create function 'SomeClass'
// ERROR: Unresolved reference: SomeClass

val x = <caret>SomeClass()