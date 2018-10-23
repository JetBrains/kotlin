// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
// IS_APPLICABLE_2: false
fun foo() {
    <caret>List<String>.contains("a")
}