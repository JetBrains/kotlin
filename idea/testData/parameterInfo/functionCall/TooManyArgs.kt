// COMPILER_ARGUMENTS: -XXLanguage:-TrailingCommas
open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        m(1, false, <caret>)
    }
}
//Text: (x: Int, y: Boolean), Disabled: true, Strikeout: false, Green: true