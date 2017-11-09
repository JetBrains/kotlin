// INTENTION_TEXT: "Add import for 'java.util.ArrayList'"
// WITH_RUNTIME

fun test() {
    val myList = java.util<caret>.ArrayList<Int>()
    val otherList = java.util.ArrayList<String>()
}
