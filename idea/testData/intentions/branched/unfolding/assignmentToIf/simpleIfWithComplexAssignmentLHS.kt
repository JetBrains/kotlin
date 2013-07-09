fun test(n: Int): Array<String> {
    var x: Array<String> = Array<String>(1, {""})

    x[0] = <caret>if (n > 5) "A" else "B"

    return x
}
