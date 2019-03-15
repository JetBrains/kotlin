// "Replace with assignment (original is empty)" "false"
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_RUNTIME
fun test(otherList: List<Int>) {
    var list: List<Int>
    list = emptyList<Int>()
    list +=<caret> otherList
}