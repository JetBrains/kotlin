// ERROR: Expected condition of type kotlin.Boolean
// ERROR: Expected condition of type kotlin.Boolean

fun test(n: Int): String {
    return <caret>when {
        is String -> "String"
        in 1..10 -> "1..10"
        else -> "unknown"
    }
}