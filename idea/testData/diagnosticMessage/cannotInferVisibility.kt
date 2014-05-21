// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: CANNOT_INFER_VISIBILITY

trait T {
    internal val foo: String
}

trait U {
    protected val foo: String
}

trait V : T, U
