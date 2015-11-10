package ppp

interface I {
    val p: CCCC.Nested
}

class CCCC(over<caret>val x: Int) : I {
    interface Nested
}

// ELEMENT_TEXT: "override val p: CCCC.Nested"
