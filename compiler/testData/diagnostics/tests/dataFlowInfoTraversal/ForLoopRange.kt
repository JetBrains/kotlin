fun foo(arr: Array<Int>?) {
    for (x in arr!!) {
        <!DEBUG_INFO_AUTOCAST!>arr<!> : Array<Int>
    }
    <!DEBUG_INFO_AUTOCAST!>arr<!> : Array<Int>
}
