// "Import" "false"
// ACTION: Create annotation 'Foo'
// ACTION: Create class 'Foo'
// ACTION: Create enum 'Foo'
// ACTION: Create interface 'Foo'
// ACTION: Create type alias 'Foo'
// ACTION: Create type parameter 'Foo' in function 'use'
// ERROR: Unresolved reference: Foo
package bar

fun use(f: <caret>Foo) {

}