interface T {
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>internal<!> var foo: Long
}

interface U {
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>protected<!> var foo: Long
}

interface V : T, U {
    <!CANNOT_INFER_VISIBILITY!>override var foo: Long<!>
}

interface <!CANNOT_INFER_VISIBILITY!>W<!> : T, U
