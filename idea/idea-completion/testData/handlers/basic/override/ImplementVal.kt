// FIR_COMPARISON
interface I {
    val someVal: String?
}

class A : I {
    o<caret>
}

// ELEMENT_TEXT: "override val someVal: String?"
