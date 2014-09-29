fun foo(arr: Array<Int>?) {
    for (x in arr!!) {
        <!DEBUG_INFO_SMARTCAST!>arr<!> : Array<Int>
    }
    <!DEBUG_INFO_SMARTCAST!>arr<!> : Array<Int>
}
