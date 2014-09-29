// "class com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase" "false"
// ACTION: Create local variable 'PrivateClass'
// ERROR: Unresolved reference: PrivateClass

fun test() {
    <caret>PrivateClass
}