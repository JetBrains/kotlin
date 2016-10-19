// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create class 'SomeTest'
// ACTION: Rename reference
// ACTION: Add dependency on module...
// ERROR: Unresolved reference: SomeTest

package testing

val x = testing.<caret>SomeTest()
