// "Import" "false"
// ACTION: Create function 'FooPackage'
// ACTION: Create class 'FooPackage'
// ACTION: Rename reference
// ERROR: Unresolved reference: FooPackage

package packageClass

fun functionImportTest() {
    <caret>FooPackage()
}
