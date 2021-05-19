// FIR_IDENTICAL
// FIR_COMPARISON
fun foo(p: Int = <caret>)

// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: fun
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: return
// EXIST: super
// EXIST: throw
// EXIST: true
// EXIST: try
// EXIST: when
// EXIST: while
// NOTHING_ELSE
