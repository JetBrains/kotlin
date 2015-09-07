// "Specify type explicitly" "false"
// ACTION: Convert property to function
// ERROR: Unresolved reference: foo

class A() {
    public val <caret>t = foo()
}