// WITH_RUNTIME
// INTENTION_TEXT: "Replace with 'contains()'"
// IS_APPLICABLE_2: false
fun foo(list: List<String>) {
    val res = list
    <caret>res.contains("a")
}