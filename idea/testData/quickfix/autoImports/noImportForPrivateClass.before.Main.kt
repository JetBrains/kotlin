// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Create local variable 'PrivateClass'
// ACTION: Create object 'PrivateClass'
// ACTION: Create parameter 'PrivateClass'
// ACTION: Create property 'PrivateClass'
// ACTION: Rename reference
// ACTION: Add dependency on module...
// ERROR: Unresolved reference: PrivateClass

fun test() {
    <caret>PrivateClass
}