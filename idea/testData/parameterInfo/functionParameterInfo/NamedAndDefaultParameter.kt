open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean = true, z: Long = 12345678901234, u: String = "abc\n", u0: String = "", uu: String = "$u", v: Char = '\u0000') = 2

    fun d(x: Int) {
        m(<caret>y = false, x = 1)
    }
}
/*
Text: ([x: Int]), Disabled: false, Strikeout: false, Green: false
Text: ([y: Boolean = true], [x: Int], [z: Long = 1234567...], [u: String = "abc\n"], [u0: String = ""], [uu: String = ?], [v: Char = '\u0000']), Disabled: false, Strikeout: false, Green: true
*/