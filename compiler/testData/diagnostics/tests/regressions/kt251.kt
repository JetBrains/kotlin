class A() {
    var x: Int = 0
        get() = <!TYPE_MISMATCH!>"s"<!>
        set(value: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            $x = <!TYPE_MISMATCH!>value<!>
        }
    val y: Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>String<!> = "s"
    val z: Int
        get() {
            return <!TYPE_MISMATCH!>"s"<!>
        }

    var a: Any = 1
        set(v: <!WRONG_SETTER_PARAMETER_TYPE!>String<!>) {
            $a = v
        }
    val b: Int
        get(): <!WRONG_GETTER_RETURN_TYPE!>Any<!> = "s"
    val c: Int
        get() {
            return 1
        }
    val d = 1
        get() {
            return $d
        }
    val e = 1
        get(): <!WRONG_GETTER_RETURN_TYPE!>String<!> {
            return <!TYPE_MISMATCH!>$e<!>
        }

}
