// "Replace with assignment (original is empty)" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// ACTION: Change type to mutable
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_RUNTIME
fun test(otherList: List<Int>) {
    var list = listOf(1, 2, 3)
    foo()
    bar()
    list <caret>+= otherList
}

fun foo() {}
fun bar() {}