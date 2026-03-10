// WITH_STDLIB

fun test(seq: Sequence<Int>): Boolean {
    val seq2 = seq.map { it + 1 }.filter { it % 2 == 0 }.map { it * 2 }
    for (item in seq2) {
        return true
    }
    return false
}

fun test2(): Boolean {
    val seq = sequenceOf(1, 2, 3)
    val seq2 = seq.map { it + 1 }.filter { it % 2 == 0 }.map { it * 2 }
    for (item in seq2) {
        return true
    }
    return false
}

fun test3(): Boolean {
    val seq = generateSequence(1) { x -> if (x < 3) x + 1 else null }
    val seq2 = seq.map { it * 2 }.filter { it % 4 != 0 }
    var enteredBody = false
    for (item in seq2) {
        enteredBody = true
    }
    if (!enteredBody) return false
    val seq3 = generateSequence({ 1 }, { x -> if (x < 3) x + 1 else null })
    val seq4 = seq3.map { it * 2 }.filter { it % 4 != 0 }
    enteredBody = false
    for (item in seq4) {
        enteredBody = true
    }
    if (!enteredBody) return false
    var k = 1
    val seq5 = generateSequence({ if (k < 3) k++ else null })
    val seq6 = seq5.map { it * 2 }.filter { it % 4 != 0 }
    enteredBody = false
    for (item in seq6) {
        return true
    }
    return false
}

fun box(): String {
    val seq = sequenceOf(1, 2, 3)
    if (!test(seq)) return "failed: sequence skipped loop body when lowering with function argument"
    if (!test2()) return "failed: sequence skipped loop body when lowering with sequenceOf"
    if (!test3()) return "failed: sequence skipped loop body when lowering with generateSequence"
    return "OK"
}