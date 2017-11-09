fun digitCountInNumber(n: Int, m: Int): Int {
    var count = 0
    var number = n
    do {
        if (m == number % 10) {
            count++
        }
        number /= 10
    } while (number > 0)
    return count
}
