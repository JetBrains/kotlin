// FIR_COMPARISON
fun foo() {
    try {

    }
    catch(e: Exception) {

    }
    <caret>
}

// EXIST: catch
// EXIST: finally
// EXIST: class
// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: fun
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: return
// EXIST: throw
// EXIST: interface
// EXIST: true
// EXIST: try
// EXIST: val
// EXIST: var
// EXIST: when
// EXIST: while
// EXIST: as
// EXIST:  typealias
// NOTHING_ELSE
