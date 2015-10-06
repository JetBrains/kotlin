fun calc(x: List<String>?): Int {
    // x should be non-null in arguments list, despite of a chain
    x?.subList(0, 1)?.get(<!DEBUG_INFO_SMARTCAST!>x<!>.size)
    // But not here!
    return x!!.size
}
