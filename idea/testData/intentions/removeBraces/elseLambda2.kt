fun test(i: Int) {
    val predicate: () -> Boolean =
        if (i == 1) {
            { true }
        } else {
            <caret>{ -> false }
        }
}