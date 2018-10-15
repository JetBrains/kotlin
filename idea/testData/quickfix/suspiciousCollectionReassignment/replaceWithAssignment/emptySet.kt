// "Replace with assignment" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test(otherList: Set<Int>) {
    var list = emptySet<Int>()
    foo()
    bar()
    list <caret>+= otherList
}

fun foo() {}
fun bar() {}