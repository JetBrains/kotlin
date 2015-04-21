// !CHECK_TYPE

package d

fun <T: Any> joinT(<!UNUSED_PARAMETER!>x<!>: Int, vararg <!UNUSED_PARAMETER!>a<!>: T): T? {
    return null
}

fun <T: Any> joinT(<!UNUSED_PARAMETER!>x<!>: Any, <!UNUSED_PARAMETER!>y<!>: T): T? {
    return null
}

fun test() {
    val x2 = joinT(<!NON_VARARG_SPREAD!>*<!>1, "2")
    checkSubtype<String?>(x2)
}

