// "Create function 'bar'" "false"
// ACTION: Create annotation 'bar'
// ACTION: Make private
// ACTION: Make public
// ERROR: Unresolved reference: foo
// ERROR: Unresolved reference: bar

@foo(1, "2", <caret>bar("3", 4)) fun test() {

}