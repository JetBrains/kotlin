// WITH_RUNTIME

fun List<Int>.decimateEveryEvenThird() = sequence {
    var counter = 1
    for (e in this@List) {
        if (e % 2 == 0 && counter % 3 == 0) {
            yield(e)
        }
        counter += 1
    }
}