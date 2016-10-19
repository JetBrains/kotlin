// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Create local variable 'Nested'
// ACTION: Create object 'Nested'
// ACTION: Create parameter 'Nested'
// ACTION: Create property 'Nested'
// ACTION: Rename reference
// ACTION: Add dependency on module...
// ERROR: Unresolved reference: Nested

fun test() {
    <caret>Nested
}