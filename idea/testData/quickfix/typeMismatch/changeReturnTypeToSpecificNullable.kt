// "Change return type of current function 'foo' to 'Int?'" "true"

fun foo(): String {
    val n: Int? = 1
    return <caret>n
}