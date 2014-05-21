trait T {
    internal var foo: Long
}

trait U {
    protected var foo: Long
}

trait V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Long<!>
}

trait <!CANNOT_INFER_VISIBILITY!>W<!> : T, U
