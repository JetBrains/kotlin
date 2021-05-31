// FIR_COMPARISON
fun foo(param: String) {
    val s = "$param.l<caret>bla-bla-bla"
}

// EXIST: { itemText: "length", attributes: "" }
// ABSENT: hashCode
// EXIST: { itemText: "lastIndex", attributes: "" }
