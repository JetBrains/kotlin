fun calc(x: List<String>?, y: List<Int>?) {
    // x and y should be non-null in arguments list, despite of a chains
    x?.subList(y?.subList(1, 2)?.get(<!DEBUG_INFO_SMARTCAST!>y<!>.size) ?: 0,
               y?.get(0) ?: 1) // But safe call is NECESSARY here for y
     ?.get(<!DEBUG_INFO_SMARTCAST!>x<!>.size)
}
