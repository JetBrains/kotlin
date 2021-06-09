// FIR_COMPARISON
fun foo(param: String) {
    val s = "$param.<caret>bla-bla-bla"
}

// EXIST: { itemText: "length", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "lowercase", attributes: "bold" }
