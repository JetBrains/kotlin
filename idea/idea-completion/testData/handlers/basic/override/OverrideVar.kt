// FIR_COMPARISON
open class B {
    open var someVar: String = ""
}

class A : B {
    o<caret>
}

// ELEMENT_TEXT: "override var someVar: String"
