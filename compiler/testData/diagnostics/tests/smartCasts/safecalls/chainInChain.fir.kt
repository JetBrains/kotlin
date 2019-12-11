fun calc(x: List<String>?, y: List<Int>?) {
    // x and y should be non-null in arguments list, despite of a chains
    x?.subList(y?.subList(1, 2)?.get(y.size) ?: 0,
               y?.get(0) ?: 1) // But safe call is NECESSARY here for y
     ?.get(x.size)
}
