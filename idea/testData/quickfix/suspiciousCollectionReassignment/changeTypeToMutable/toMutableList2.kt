// "Change type to mutable" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test(a: Any) {
    var list = a as List<Int>
    list -=<caret> 2
}