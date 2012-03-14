open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean = true, z: Long = 12345678901234) = 2

    fun d(x: Int) {
        m(<caret>y = false, x = 1)
    }
}
/*
Text: ([x: jet.Int]), Disabled: false, Strikeout: false, Green: false
Text: ([y: jet.Boolean = true], [x: jet.Int], [z: jet.Long = 1234567...]), Disabled: false, Strikeout: false, Green: true
*/