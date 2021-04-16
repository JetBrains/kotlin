// "Import" "false"
// ERROR: Unresolved reference: SomeTest
// ERROR: Expression expected, but a package name found
// ACTION: Convert to lazy property
// ACTION: Convert property initializer to getter
// ACTION: Create class 'SomeTest'
// ACTION: Replace safe access expression with 'if' expression
// ACTION: Rename reference

package testing

val x = testing?.<caret>SomeTest()
