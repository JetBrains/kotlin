//kt-411 Wrong type expected when returning from a function literal

package kt411

fun f() {
    invoker(
    @{
        <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@ 11<!> // expects Function, but should expect Int
    }
    )
}
fun invoker(<!UNUSED_PARAMETER!>gen<!> : () -> Int) : Int  = 0

//more tests
fun t1() {
    val <!UNUSED_VARIABLE!>v<!> = @{ () : Int ->
        return@ 111
    }
}

fun t2() : String {
    val <!UNUSED_VARIABLE!>g<!> : ()-> Int = @{
        if (true) {
            <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@ 1<!>
        }
        <!RETURN_NOT_ALLOWED!>return "s"<!>
    }
    return "s"
}

fun t3() : String {
    invoker(
    @{
        if (true) {
            <!RETURN_NOT_ALLOWED!>return@t3 "1"<!>
        }
        else {
            <!RETURN_NOT_ALLOWED!>return <!ERROR_COMPILE_TIME_VALUE!>2<!><!>
        }
        <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@ 0<!>
    }
    )
    invoker(
    @{ (): Int ->
        return@ 1
    }
    )
    invoker(
    {
        <!RETURN_NOT_ALLOWED!>return "0"<!>
    }
    )
    return "2"
}

fun t4() : Int {
    val <!UNUSED_VARIABLE!>h<!> :  ()-> String = @l{
        <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return@l "a"<!>
    }
    val <!UNUSED_VARIABLE!>g<!> :  ()-> String = @{ () : String ->
        return@ "a"
    }

    fun inner(): String {
        return "2"
    }

    return 12
}