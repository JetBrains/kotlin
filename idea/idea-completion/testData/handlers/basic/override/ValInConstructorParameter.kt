// FIR_COMPARISON
interface I {
    val someVal: java.io.File?
}

class A(ov<caret>) : I {
}

// ELEMENT_TEXT: "override val someVal: File?"
