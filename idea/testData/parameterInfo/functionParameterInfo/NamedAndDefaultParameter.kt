open class A(x: Int) {
    fun m(x: Int) = 1
    fun m(x: Int, y: Boolean = true, z: Long = 12345678901234, u: String = "abc\n", u0: String = "" + "123", uu: String = "$u", v: Char = '\u0000', vv: String = "asdfsdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdfasdf") = 2

    fun d(x: Int) {
        m(<caret>y = false, x = 1)
    }
}
/*
Text: ([x: Int]), Disabled: false, Strikeout: false, Green: false
Text: ([y: Boolean = true], [x: Int], [z: Long = 12345678901234], [u: String = "abc\n"], [u0: String = "" + "123"], [uu: String = "$u"], [v: Char = '\u0000'], [vv: String = "..."]), Disabled: false, Strikeout: false, Green: true
*/