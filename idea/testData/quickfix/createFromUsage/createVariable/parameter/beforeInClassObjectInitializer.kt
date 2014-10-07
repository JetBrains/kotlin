// "Create parameter 'foo'" "false"
// ACTION: Create local variable 'foo'
// ACTION: Split property declaration
// ERROR: Unresolved reference: foo

class A {
    class object {
        {
            val t: Int = <caret>foo
        }
    }
}