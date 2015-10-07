fun calc(x: List<String>?) {
    // x should be non-null in arguments list, despite of a chain
    x?.subList(0, <!DEBUG_INFO_SMARTCAST!>x<!>.size)?.
       subList(0, <!DEBUG_INFO_SMARTCAST!>x<!>.size)?.
       get(<!DEBUG_INFO_SMARTCAST!>x<!>.size)
}
