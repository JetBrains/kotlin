// IS_APPLICABLE: false
fun test(i: Int) {
    val predicate: () -> Boolean =
        if (i == 1) {
            <caret>{ true }
        } else {
            { false }
        }
}