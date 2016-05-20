fun foo() {
    try {

    }
    <caret>
}

// EXIST: catch
// EXIST: finally
// EXIST: false
// EXIST: null
// EXIST: true
// NOTHING_ELSE
