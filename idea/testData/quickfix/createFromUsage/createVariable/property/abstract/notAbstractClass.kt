// "Create abstract property 'foo'" "false"
// ACTION: Convert to expression body
// ACTION: Create local variable 'foo'
// ACTION: Create parameter 'foo'
// ACTION: Create property 'foo'
// ACTION: Create property 'foo' as constructor parameter
// ACTION: Rename reference
// ERROR: Unresolved reference: foo
class A {
    fun bar(b: Boolean) {}

    fun test() {
        bar(<caret>foo)
    }
}