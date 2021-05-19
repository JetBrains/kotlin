// FIR_COMPARISON
fun foo(p: Int) {
    x()
    y()
    val edge = a.b(<caret>)
    z()
}

// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: fun
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: return
// EXIST: throw
// EXIST: true
// EXIST: try
// EXIST: when
// EXIST: while
// NOTHING_ELSE
