// "Create abstract function 'foo'" "false"
// ACTION: Create extension function 'B.foo'
// ACTION: Create member function 'B.foo'
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
abstract class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(B().<caret>foo(1, "2"))
    }
}

class B {

}