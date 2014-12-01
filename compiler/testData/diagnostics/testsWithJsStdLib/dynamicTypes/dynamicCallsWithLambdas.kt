fun test(d: dynamic) {
    d.foo {}

    d.foo { <!UNRESOLVED_REFERENCE!>it<!> }

    d.foo { <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> }

    d.foo { (x: Int) -> "" }

    d.foo { <!CANNOT_INFER_PARAMETER_TYPE!>x<!>, <!CANNOT_INFER_PARAMETER_TYPE!>y<!> -> "" }

    d.foo { (x: String, y: Int) -> "" }
}
