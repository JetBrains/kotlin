open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        m(<caret>y = false, x = 1)
    }
}
/*
Text: (<highlight>[y: Boolean]</highlight>, [x: Int]), Disabled: false, Strikeout: false, Green: true
Text: ([x: Int]), Disabled: true, Strikeout: false, Green: false
*/