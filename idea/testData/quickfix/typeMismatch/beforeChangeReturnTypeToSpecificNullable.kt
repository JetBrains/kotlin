// "Change 'foo' function return type to 'Int?'" "true"

fun foo(): String {
    val n: Int? = 1
    return <caret>n
}