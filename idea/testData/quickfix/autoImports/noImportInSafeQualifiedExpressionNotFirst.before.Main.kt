// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ERROR: Unresolved reference: SomeTest
// ACTION: Disable 'OperatorToFunctionIntention'
// ACTION: Edit intention settings
// ACTION: Replace safe access expression with 'if' expression
// ACTION: Edit intention settings
// ACTION: Replace overloaded operator with function call
// ACTION: Disable 'Replace Safe Access Expression With 'if' Expression'

package testing

val x = testing?.<caret>SomeTest()
