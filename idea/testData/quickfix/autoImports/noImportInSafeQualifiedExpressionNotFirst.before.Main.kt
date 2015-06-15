// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ERROR: Unresolved reference: SomeTest
// ACTION: Create class 'SomeTest'
// ACTION: Replace safe access expression with 'if' expression

package testing

val x = testing?.<caret>SomeTest()
