fun testValLabelInReturn() {
    label@ val fn = { return@lab<caret>el }
    fn()
}
