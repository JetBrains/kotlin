// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo() {
    var list1 = java.util.Collections.emptyList<String>()
    val list2 = listOf("b")
    list1 <caret>= list1 + list2
}