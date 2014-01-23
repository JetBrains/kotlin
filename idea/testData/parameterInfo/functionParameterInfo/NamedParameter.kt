open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        m(x = 1, <caret>y = false)
    }
}
/*
Text: ([x: Int]), Disabled: true, Strikeout: false, Green: false
Text: ([x: Int], <highlight>[y: Boolean]</highlight>), Disabled: false, Strikeout: false, Green: true
*/