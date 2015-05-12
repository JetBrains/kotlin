interface T {
    internal var foo: Long
}

interface U {
    protected var foo: Long
}

interface V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Long<!>
}

interface <!CANNOT_INFER_VISIBILITY!>W<!> : T, U
