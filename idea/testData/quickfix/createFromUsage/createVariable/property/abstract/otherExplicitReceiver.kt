// "Create abstract property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create extension property 'B.foo'
// ACTION: Create member property 'B.foo'
// ACTION: Create property 'foo' as constructor parameter
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
abstract class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(B().<caret>foo)
    }
}

class B {

}