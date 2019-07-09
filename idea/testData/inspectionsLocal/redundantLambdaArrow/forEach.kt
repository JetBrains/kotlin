// PROBLEM: none
// WITH_RUNTIME

fun test() {
    // We do not report "redundant arrow" here,
    // because it's used to explicitly call forEach with unused lambda parameter
    listOf(1, 2, 3).forEach { <caret>_ -> }
}