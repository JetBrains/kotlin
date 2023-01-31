fun foo() = withIntList {
    withStringSequence {
        forEach { line ->
            line.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>rem<!>(1)
            line.length
        }
    }
}

fun withIntList(x: List<Int>.() -> Unit) {}

fun <T> withStringSequence(action: Sequence<String>.() -> T): T = TODO()
