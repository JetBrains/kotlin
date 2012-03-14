open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 2
    fun m(x: Int) = 1

    fun d(x: Int) {
        m(<caret>1, false, false)
    }
}
/*
Text: (x: jet.Int), Disabled: false, Strikeout: false, Green: false
Text: (x: jet.Int, y: jet.Boolean), Disabled: false, Strikeout: false, Green: false
*/