// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create class 'ExcludedClass'
// ACTION: Create function 'ExcludedClass'
// ACTION: Rename reference
// ERROR: Unresolved reference: ExcludedClass

val x = <caret>ExcludedClass()