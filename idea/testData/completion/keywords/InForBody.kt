fun foo() {
    for (i in 1..10) <caret>
}

// EXIST: break
// EXIST: continue
// EXIST: do
// EXIST: false
// EXIST: for
// EXIST: if
// EXIST: null
// EXIST: object
// EXIST: package
// EXIST: return
// EXIST: super
// EXIST: this
// EXIST: throw
// EXIST: true
// EXIST: try
// EXIST: when
// EXIST: while
// EXIST: yield
// NUMBER: 18