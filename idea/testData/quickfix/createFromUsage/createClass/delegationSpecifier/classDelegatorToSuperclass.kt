// "Create class 'A'" "false"
// ACTION: Create interface 'A'
// ACTION: Create type alias 'A'
// ACTION: Create test
// ERROR: Unresolved reference: A
package p

class Foo: <caret>A {

}