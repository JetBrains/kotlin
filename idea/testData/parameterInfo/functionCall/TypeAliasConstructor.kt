class A(val a: Int) {
    constructor(val b: String) : this(b.toInt())
}

typealias TA = A

fun usage() {
    val x = TA(<caret>)
}

/*
Text: (<highlight>a: Int</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>b: String</highlight>), Disabled: false, Strikeout: false, Green: false
 */