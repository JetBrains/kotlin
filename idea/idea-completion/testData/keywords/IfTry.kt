fun foo(p: Int) {
    if (p > 0)
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
