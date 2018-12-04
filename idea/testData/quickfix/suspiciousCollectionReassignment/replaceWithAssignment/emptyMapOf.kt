// "Replace with assignment (original is empty)" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.SuspiciousCollectionReassignmentInspection
// WITH_RUNTIME
fun test(otherMap: Map<Int, Int>) {
    var list = mapOf<Int, Int>()
    foo()
    bar()
    list <caret>+= otherMap
}

fun foo() {}
fun bar() {}