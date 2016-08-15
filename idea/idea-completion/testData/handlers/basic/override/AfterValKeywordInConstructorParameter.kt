interface I {
    val someVal: java.io.File?
}

class A(override val s<caret>) : I {
}

// ELEMENT_TEXT: "override val someVal: File?"
