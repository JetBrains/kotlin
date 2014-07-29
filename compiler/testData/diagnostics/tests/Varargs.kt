fun v(<!UNUSED_PARAMETER!>x<!> : Int, <!UNUSED_PARAMETER!>y<!> : String, vararg <!UNUSED_PARAMETER!>f<!> : Long) {}
fun v1(vararg <!UNUSED_PARAMETER!>f<!> :  (Int) -> Unit) {}

fun test() {
    v(1, "")
    v(1, "", 1)
    v(1, "", 1, 1, 1)
    v(1, "", 1, 1, 1)

    v1()
    v1({})
    v1({}, {})
    v1({}, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, {})
    v1({}, {}, {<!UNUSED_EXPRESSION!>it<!>})
    v1({}) <!VARARG_OUTSIDE_PARENTHESES, DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>
    v1 <!VARARG_OUTSIDE_PARENTHESES, DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>
}