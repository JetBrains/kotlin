fun foo(p: Int) {
    x()
    y()
    val edge = a.b(<caret>)
    z()
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
// NUMBER: 17
