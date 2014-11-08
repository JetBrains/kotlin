// IS_APPLICABLE: false
// WITH_RUNTIME
fun foo(): List<Int> {
    return <caret>for (x in 1..10) yield x*x
}