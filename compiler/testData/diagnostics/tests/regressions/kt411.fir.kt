//kt-411 Wrong type expected when returning from a function literal

package kt411

fun f() {
    invoker(
    l@{
        return@l 11 // expects Function, but should expect Int
    }
    )
}
fun invoker(gen : () -> Int) : Int  = 0

//more tests
fun t1() {
    val v = l@{
        return@l 111
    }
}

fun t2() : String {
    val g : ()-> Int = l@{
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
            <!RETURN_NOT_ALLOWED!>return<!> <!RETURN_TYPE_MISMATCH!>2<!>
        }
        return@l 0
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
    val h :  ()-> String = l@{
        return@l "a"
    }
    val g :  ()-> String = l@{
        return@l "a"
    }

    fun inner(): String {
        return "2"
    }

    return 12
}
