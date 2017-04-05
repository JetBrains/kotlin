// "Remove single lambda parameter declaration" "false"
// ACTION: Move lambda argument into parentheses
// ACTION: Rename to _
// ACTION: Specify explicit lambda signature
// ACTION: Specify type explicitly
// WITH_RUNTIME

fun main() {
    mapOf(1 to 2).forEach { t, <caret>u -> println(t) }
}