// FIR_COMPARISON
fun foo() {
    try {

    }
    finally {

    }
    <caret>
}

// EXIST: class
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
// EXIST: interface
// EXIST: true
// EXIST: try
// EXIST: val
// EXIST: var
// EXIST: when
// EXIST: while
// EXIST:  typealias
// NOTHING_ELSE
