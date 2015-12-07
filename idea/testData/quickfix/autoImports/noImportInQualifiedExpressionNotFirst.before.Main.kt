// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create class 'SomeTest'
// ERROR: Unresolved reference: SomeTest

package testing

val x = testing.<caret>SomeTest()
