// "Create class 'A'" "false"
// ACTION: Create interface 'A'
// ACTION: Create type alias 'A'
// ACTION: Create type parameter 'A' in class 'Foo'
// ACTION: Create test
// ERROR: Unresolved reference: A
package p

class Foo: <caret>A {

}