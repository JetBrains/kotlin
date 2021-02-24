package continuation

fun main() {
    val a = "a"
    fibonacci().take(10).toList()
}

fun nextSequence(terms: Pair<Int, Int>): Pair<Int, Int> {
    val terms1 = Pair(terms.second, terms.first + terms.second)
    if (terms1.first == 8) {
        //Breakpoint!
        return terms1
    } else
        return terms1
}

fun fibonacci() = sequence {
    var terms = Pair(0, 1)
    var step = 0

    while (true) {
        yield(terms.first)
        terms = nextSequence(terms)
        step++
    }
}