// "Make 'foo' protected" "false"
// ACTION: Make 'foo' public
// ACTION: Make 'foo' internal
// ACTION: Introduce local variable
// ERROR: Cannot access 'foo': it is private in 'A'

class A {
    private val foo = 1
}

class B {
    fun bar() {
        A().<caret>foo
    }
}