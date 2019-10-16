fun one() = "one"
fun String.two() = this + "two"
fun String.three() = this + "three"

fun main(args: Array<String>) {
    val s = <caret>one().two().three() // Can't select 'one()' with Alt in debugger, only 'one().two()' and 'one().two().three()' are available
}

// EXPECTED: one()