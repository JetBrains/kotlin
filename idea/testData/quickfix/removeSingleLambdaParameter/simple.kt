// "Remove single lambda parameter declaration" "true"
// WITH_RUNTIME

fun main() {
    listOf(1).forEach { <caret>x -> println() }
}