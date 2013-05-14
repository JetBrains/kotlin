fun test(n: Int): Array<String> {
    var x: Array<String> = Array<String>(1, {""})

    x[0] = <caret>when(n) {
        in 0..10 -> "small"
        in 10..100 -> "average"
        in 100..1000 -> "big"
        else -> "unknown"
    }

    return x
}
