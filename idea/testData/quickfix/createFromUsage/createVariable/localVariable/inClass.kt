// "Create local variable 'foo'" "false"
// ACTION: Create parameter 'foo'
// ACTION: Create property 'foo'
// ERROR: Unresolved reference: foo

class A {
    val t: Int = <caret>foo
}