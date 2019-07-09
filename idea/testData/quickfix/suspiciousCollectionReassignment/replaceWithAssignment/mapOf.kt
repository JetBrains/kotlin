// "Replace with assignment (original is empty)" "false"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// ACTION: Change type to mutable
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with ordinary assignment
// WITH_RUNTIME
fun test(otherMap: Map<Int, Int>) {
    var list = mapOf<Int, Int>(1 to 1, 2 to 2)
    foo()
    bar()
    list <caret>+= otherMap
}

fun foo() {}
fun bar() {}