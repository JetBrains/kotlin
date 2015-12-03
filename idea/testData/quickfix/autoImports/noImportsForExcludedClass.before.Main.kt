// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create class 'ExcludedClass'
// ACTION: Create function 'ExcludedClass'
// ACTION: Rename reference
// ERROR: Unresolved reference: ExcludedClass

val x = <caret>ExcludedClass()