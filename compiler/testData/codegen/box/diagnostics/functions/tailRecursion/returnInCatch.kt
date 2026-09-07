
<!NO_TAIL_CALLS_FOUND_IN_IR!>tailrec<!> fun test(counter : Int) : Int {
    if (counter == 0) return 0

    try {
        throw Exception()
    } catch (e : Exception) {
        return test(counter - 1)
    }
}

fun box() : String = if (test(3) == 0) "OK" else "FAIL"
