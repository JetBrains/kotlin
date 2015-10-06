// "class org.jetbrains.kotlin.idea.quickfix.AutoImportFix" "false"
// ACTION: Create local variable 'PrivateClass'
// ACTION: Create object 'PrivateClass'
// ACTION: Create parameter 'PrivateClass'
// ACTION: Create property 'PrivateClass'
// ERROR: Unresolved reference: PrivateClass

fun test() {
    <caret>PrivateClass
}