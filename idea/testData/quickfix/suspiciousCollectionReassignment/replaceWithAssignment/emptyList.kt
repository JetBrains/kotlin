// "Replace with assignment (original is empty)" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test(otherList: List<Int>) {
    var list = emptyList<Int>()
    foo()
    bar()
    list <caret>+= otherList
}

fun foo() {}
fun bar() {}