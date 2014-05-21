trait T {
    public var foo: Short
        internal set
}

trait U {
    public var foo: Short
        protected set
}

trait V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Short<!>
}
