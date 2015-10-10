class A {
    operator fun get(x: String) = 1
    operator fun get(x: String, y: Boolean) = 2
    operator fun get(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        this[1, <caret>false]
    }
}
/*
Text: (x: Int, <highlight>y: Boolean</highlight>), Disabled: false, Strikeout: false, Green: true
Text: (x: String), Disabled: true, Strikeout: false, Green: false
Text: (x: String, <highlight>y: Boolean</highlight>), Disabled: true, Strikeout: false, Green: false
*/