fun factorial(i: Int): Int {
    var result = 1
    for (j in 2..i) {
        result *= j
    }
    return result
}