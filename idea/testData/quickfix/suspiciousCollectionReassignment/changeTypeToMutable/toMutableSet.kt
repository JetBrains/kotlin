// "Change type to mutable" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test() {
    var set = foo()
    set -=<caret> 1
}

fun foo() = setOf(1)