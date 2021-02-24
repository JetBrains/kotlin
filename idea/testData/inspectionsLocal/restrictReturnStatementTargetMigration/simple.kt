// LANGUAGE_VERSION: 1.4
// PROBLEM: none
// ERROR: Target label does not denote a function

fun testValLabelInReturn() {
    <caret>L@ val fn = { return@L }
    fn()
}