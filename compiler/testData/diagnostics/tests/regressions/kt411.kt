//kt-411 Wrong type expected when returning from a function literal

package kt411

fun f() {
    invoker(
    l@{
        return@l 11 // expects Function, but should expect Int
    }
    )
}
fun invoker(<!UNUSED_PARAMETER!>gen<!> : () -> Int) : Int  = 0

//more tests
fun t1() {
    val <!UNUSED_VARIABLE!>v<!> = l@{
        return@l 111
    }
}

fun t2() : String {
    val <!UNUSED_VARIABLE!>g<!> : ()-> Int = l@{
        if (true) {
            return@l 1
        }
        <!RETURN_NOT_ALLOWED!>return<!> "s"
    }
    return "s"
}

fun t3() : String {
    invoker(
    l@{
        if (true) {
            <!RETURN_NOT_ALLOWED!>return@t3<!> "1"
        }
        else {
            <!RETURN_NOT_ALLOWED!>return<!> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>
        }
        <!UNREACHABLE_CODE!>return@l 0<!>
    }
    )
    invoker(
    l@{
        return@l 1
    }
    )
    invoker(
    {
        <!RETURN_NOT_ALLOWED!>return<!> "0"
    }
    )
    return "2"
}

fun t4() : Int {
    val <!UNUSED_VARIABLE!>h<!> :  ()-> String = l@{
        return@l "a"
    }
    val <!UNUSED_VARIABLE!>g<!> :  ()-> String = l@{
        return@l "a"
    }

    fun inner(): String {
        return "2"
    }

    return 12
}