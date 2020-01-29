// LANGUAGE_VERSION: 1.4
// DISABLE-ERRORS

fun testValLabelInReturn() {
    <caret>L@ val fn = { return@L }
    fn()
}