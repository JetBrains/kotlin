// !CHECK_TYPE

fun foo(arr: Array<Int>?) {
    for (x in arr!!) {
        checkSubtype<Array<Int>>(<!DEBUG_INFO_SMARTCAST!>arr<!>)
    }
    checkSubtype<Array<Int>>(<!DEBUG_INFO_SMARTCAST!>arr<!>)
}
