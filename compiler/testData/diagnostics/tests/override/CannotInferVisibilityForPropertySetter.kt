interface T {
    public var foo: Short
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>internal<!> set
}

interface U {
    public var foo: Short
        <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>protected<!> set
}

interface V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Short<!>
}
