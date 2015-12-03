// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ERROR: Unresolved reference: SomeTest
// ERROR: Expression expected, but a package name found
// ACTION: Create class 'SomeTest'
// ACTION: Replace safe access expression with 'if' expression
// ACTION: Rename reference

package testing

val x = testing?.<caret>SomeTest()
