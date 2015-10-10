open class A(x: Int) {
    protected constructor() : this(1) {}
    private constructor(p: String) : this(2) {}
}

class B(): A(<caret>5)

/*
Text: (<highlight>x: Int</highlight>), Disabled: false, Strikeout: false, Green: true
Text: (<no parameters>), Disabled: true, Strikeout: false, Green: false
*/