// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ERROR: Unresolved reference: FooPackage
// ACTION: Disable 'OperatorToFunctionIntention'
// ACTION: Edit intention settings
// ACTION: Replace overloaded operator with function call

package packageClass

fun functionImportTest() {
    <caret>FooPackage()
}
