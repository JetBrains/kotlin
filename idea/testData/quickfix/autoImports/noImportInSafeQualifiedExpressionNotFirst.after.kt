// "Import" "false"
// ERROR: Unresolved reference: SomeTest
// ERROR: Expression expected, but a package name found
// ACTION: Edit intention settings
// ACTION: Replace safe access expression with 'if' expression
// ACTION: Disable 'Replace Safe Access Expression with 'if' Expression'

package testing

val x = testing?.<caret>SomeTest()