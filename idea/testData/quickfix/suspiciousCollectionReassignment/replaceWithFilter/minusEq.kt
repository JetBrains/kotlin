// "Replace with filter" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test() {
    var list = listOf(1, 2, 3)
    list -=<caret> listOf(2)
}