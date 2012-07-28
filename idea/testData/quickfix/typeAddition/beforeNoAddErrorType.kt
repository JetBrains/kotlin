// "Specify type explicitly" "false"
// ERROR: Public or protected member should have specified type
// ERROR: Unresolved reference: foo

class A() {
    public val <caret>t = foo()
}