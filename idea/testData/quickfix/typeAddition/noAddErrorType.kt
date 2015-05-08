// "Specify type explicitly" "false"
// ACTION: Convert property to function
// ERROR: Public or protected member should have specified type
// ERROR: Unresolved reference: foo

class A() {
    public val <caret>t = foo()
}