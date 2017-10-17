fun test(i: Int) {
    val x = <caret>when {
        i == 1 -> 1
        false -> 2
        true -> 3
        else -> 4
    }
}