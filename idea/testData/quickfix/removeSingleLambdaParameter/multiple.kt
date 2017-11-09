// "Remove single lambda parameter declaration" "false"
// ACTION: Move lambda argument into parentheses
// ACTION: Rename to _
// ACTION: Specify explicit lambda signature
// ACTION: Specify type explicitly
// RUNTIME_WITH_FULL_JDK

fun main() {
    mapOf(1 to 2).forEach { t, <caret>u -> println(t) }
}