// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ACTION: Create local variable 'PrivateClass'
// ACTION: Create parameter 'PrivateClass'
// ERROR: Unresolved reference: PrivateClass

fun test() {
    <caret>PrivateClass
}