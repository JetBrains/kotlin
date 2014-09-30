// "Create parameter 'foo'" "false"
// ERROR: Unresolved reference: foo

class A {
    class object {
        val test: Int = <caret>foo
    }
}