// IS_APPLICABLE: false
// ERROR: Unresolved reference: LinkedList
fun foo() {
    val x = <caret>bar<String>()
}

fun <T> bar() : List<T> = LinkedList<T>();