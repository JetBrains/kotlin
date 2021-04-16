// "Import" "false"
// ACTION: Create local variable 'PrivateClass'
// ACTION: Create object 'PrivateClass'
// ACTION: Create parameter 'PrivateClass'
// ACTION: Create property 'PrivateClass'
// ACTION: Rename reference
// ERROR: Unresolved reference: PrivateClass

fun test() {
    <caret>PrivateClass
}