// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create class 'SomeClass'
// ACTION: Create function 'SomeClass'
// ACTION: Rename reference
// ERROR: Unresolved reference: SomeClass

val x = <caret>SomeClass()