// LANGUAGE_VERSION: 1.4
// SKIP_ERRORS_AFTER

fun testValLabelInReturn() {
    L@ val fn = { return@L<caret> }
    fn()
}