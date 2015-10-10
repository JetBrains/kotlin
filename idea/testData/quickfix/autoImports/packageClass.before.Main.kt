// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create function 'FooPackage'
// ACTION: Create class 'FooPackage'
// ERROR: Unresolved reference: FooPackage

package packageClass

fun functionImportTest() {
    <caret>FooPackage()
}
