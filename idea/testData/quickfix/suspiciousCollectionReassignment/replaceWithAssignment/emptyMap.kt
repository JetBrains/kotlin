// "Replace with assignment" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test(otherMap: Map<Int, Int>) {
    var list = emptyMap<Int, Int>()
    foo()
    bar()
    list <caret>+= otherMap
}

fun foo() {}
fun bar() {}