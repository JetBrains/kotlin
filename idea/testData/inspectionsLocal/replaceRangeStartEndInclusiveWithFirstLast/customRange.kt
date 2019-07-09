// PROBLEM: none

class MyRange : ClosedRange<String> {
    override val start: String get() = "a"
    override val endInclusive: String = "z"
}

fun main() {
    val range = MyRange()
    val start = range.<caret>endInclusive
}