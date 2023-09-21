class A() {
    var x: Int = 0
        get() = <!RETURN_TYPE_MISMATCH!>"s"<!>
        set(value: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            field = <!ASSIGNMENT_TYPE_MISMATCH!>value<!>
        }
    val y: Int
        get(): <!WRONG_GETTER_RETURN_TYPE("kotlin.Int; kotlin.String")!>String<!> = "s"
    val z: Int
        get() {
            return <!RETURN_TYPE_MISMATCH!>"s"<!>
        }

    var a: Any = 1
        set(v: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            field = v
        }
    val b: Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>Any<!> = "s"
    val c: Int
        get() {
            return 1
        }
    val d = 1
        get() {
            return field
        }
    val e = 1
        get(): <!WRONG_GETTER_RETURN_TYPE!>String<!> {
            return <!RETURN_TYPE_MISMATCH!>field<!>
        }

}
