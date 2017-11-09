// "Specify type explicitly" "false"
// ACTION: Add getter
// ACTION: Convert property to function
// ACTION: Introduce backing property
// ACTION: Move to companion object
// ACTION: Move to constructor
// ERROR: Unresolved reference: foo

class A() {
    public val <caret>t = foo()
}