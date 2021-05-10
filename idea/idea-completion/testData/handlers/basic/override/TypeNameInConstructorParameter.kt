// FIR_COMPARISON
interface I {
    protected var someVar: java.io.File?
}

class A(public som<caret>) : I {
}

// ELEMENT_TEXT: "override var someVar: File?"
