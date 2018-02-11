// ERROR: 'when' expression must be exhaustive, add necessary 'else' branch
fun test() {
    val x = <caret>when {
        false -> 1
    }
}