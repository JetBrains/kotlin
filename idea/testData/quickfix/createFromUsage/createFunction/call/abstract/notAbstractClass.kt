// "Create abstract function 'foo'" "false"
// ACTION: Create function 'foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo(1, "2"))
    }
}