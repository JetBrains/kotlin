// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'

class A {
    val t: Int get() = <caret>foo
}