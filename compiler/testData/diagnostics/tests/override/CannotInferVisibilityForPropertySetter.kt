interface T {
    public var foo: Short
        internal set
}

interface U {
    public var foo: Short
        protected set
}

interface V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Short<!>
}
