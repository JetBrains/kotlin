// FIR_IDENTICAL
// FIR_COMPARISON
fun foo() {
    Math.pow(2.0 <caret>)
}

// EXIST: !in
// EXIST: !is
// EXIST: as
// EXIST: in
// EXIST: is
// NOTHING_ELSE
