// FIR_COMPARISON
interface I {
    var someVar: String
}

class A : I {
    o<caret>
}

// ELEMENT_TEXT: "override var someVar: String"
