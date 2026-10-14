tailrec fun countDown(n: Int): Int {
    if (n <= 0) return 0
    run {
        return countDown(n - 1)
    }
}

tailrec fun noTailRecCalls() {}

tailrec fun nonTailRecCall() {
    nonTailRecCall()
    val i = 1
}

tailrec fun tailRecInTry() {
    try {
        tailRecInTry()
    } catch (e: Exception) {
        tailRecInTry()
    } finally {
        tailRecInTry()
    }
}
