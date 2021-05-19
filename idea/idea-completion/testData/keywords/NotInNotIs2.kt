// FIR_IDENTICAL
// FIR_COMPARISON
fun foo() {
    list.filter { it !i<caret> }
}

// EXIST: in
// EXIST: is
// NOTHING_ELSE
