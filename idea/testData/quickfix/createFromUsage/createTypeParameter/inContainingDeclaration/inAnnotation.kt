// "Create type parameter 'Test' in class 'C'" "false"
// ACTION: Add 'testng' to classpath
// ACTION: Convert to expression body
// ACTION: Create annotation 'Test'
// ACTION: Make internal
// ACTION: Make private
// ACTION: Make protected
// ERROR: Unresolved reference: Test
class C {
    @<caret>Test fun foo() {

    }
}