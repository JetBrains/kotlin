// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Create property 'foo'
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

class A {
    default object {
        {
            val t: Int = <caret>foo
        }
    }
}