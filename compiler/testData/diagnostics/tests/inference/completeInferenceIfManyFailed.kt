package d

fun <T> joinT(<!UNUSED_PARAMETER!>x<!>: Int, vararg <!UNUSED_PARAMETER!>a<!>: T): T? {
    return null
}

fun <T> joinT(<!UNUSED_PARAMETER!>x<!>: Any, <!UNUSED_PARAMETER!>y<!>: T): T? {
    return null
}

fun test() {
    val x2 = joinT(<!NON_VARARG_SPREAD!>*<!>1, "2")
    x2 : String?
}

