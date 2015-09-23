// "Specify type explicitly" "false"
// ACTION: Convert property to function
// ACTION: Introduce backing property
// ERROR: Unresolved reference: foo

class A() {
    public val <caret>t = foo()
}