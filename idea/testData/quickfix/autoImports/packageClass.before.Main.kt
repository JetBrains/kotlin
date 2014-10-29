// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ACTION: Create function 'FooPackage'
// ERROR: Unresolved reference: FooPackage

package packageClass

fun functionImportTest() {
    <caret>FooPackage()
}
