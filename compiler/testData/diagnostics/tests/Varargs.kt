fun v(x : Int, y : String, vararg f : Long) {}
fun v1(vararg f :  (Int) -> Unit) {}

fun test() {
    v(1, "")
    v(1, "", 1)
    v(1, "", 1, 1, 1)
    v(1, "", 1, 1, 1)

    v1()
    v1({})
    v1({}, {})
    v1({}, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>, {})
    v1({}, {}, {it})
    v1({}) <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
    v1 <!VARARG_OUTSIDE_PARENTHESES!>{}<!>
}
