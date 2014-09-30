// "Create local variable 'foo'" "false"
// ACTION: Create parameter 'foo'
// ACTION: Create property 'foo' from usage
// ERROR: Unresolved reference: foo

class A {
    val t: Int = <caret>foo
}