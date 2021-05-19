// FIR_IDENTICAL
// FIR_COMPARISON
val prop: Int
    get() = <caret>

// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: fun
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: super
// EXIST: throw
// EXIST: true
// EXIST: try
// EXIST: when
// EXIST: while
// NOTHING_ELSE
