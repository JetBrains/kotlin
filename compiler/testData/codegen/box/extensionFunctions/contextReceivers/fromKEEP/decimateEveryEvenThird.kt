// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
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

fun box() = with(listOf(0, 1, 2).decimateEveryEvenThird()) {
    if (toList() == listOf(2)) "OK" else "fail"
}