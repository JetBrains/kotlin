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
    v1({}, <!ERROR_COMPILE_TIME_VALUE!>1<!>, {})
    v1({}, {}, {it})
    v1({}) <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    v1 <!VARARG_OUTSIDE_PARENTHESES, DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>
}
