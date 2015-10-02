interface I {
    var someVar: java.io.File?
}

class A(som<caret>) : I {
}

// ELEMENT_TEXT: "override var someVar: File?"
