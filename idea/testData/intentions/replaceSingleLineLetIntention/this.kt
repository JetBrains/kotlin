// WITH_RUNTIME
// IS_APPLICABLE: true

fun Int.foo(): Int {
    return <caret>let { it.hashCode().hashCode() }
}