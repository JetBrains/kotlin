package ppp

interface I {
    val p: Nested

    interface Nested
}

class CCCC(over<caret>val x: Int) : I {
}

// ELEMENT_TEXT: "override val p: I.Nested"
