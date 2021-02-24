// FIR_COMPARISON
fun foo(param: String) {
    val s = "${<caret>}"
}

// EXIST: foo
// EXIST: param
