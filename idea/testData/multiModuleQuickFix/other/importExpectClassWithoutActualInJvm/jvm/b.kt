// "Import" "false"
// ACTION: Create annotation 'Foo'
// ACTION: Create class 'Foo'
// ACTION: Create enum 'Foo'
// ACTION: Create interface 'Foo'
// ACTION: Create type parameter 'Foo' in function 'use'
// ACTION: Enable a trailing comma by default in the formatter
// ERROR: Unresolved reference: Foo
package bar

fun use(f: <caret>Foo) {

}