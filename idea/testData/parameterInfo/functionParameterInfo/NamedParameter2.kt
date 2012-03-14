open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        m(<caret>y = false, x = 1)
    }
}
/*
Text: ([x: jet.Int]), Disabled: false, Strikeout: false, Green: false
Text: ([y: jet.Boolean], [x: jet.Int]), Disabled: false, Strikeout: false, Green: true
*/