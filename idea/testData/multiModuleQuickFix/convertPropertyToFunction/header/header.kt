// "Convert property to function" "true"
// SHOULD_FAIL_WITH: Property has an actual declaration in the class constructor

expect class Main {
    val name<caret>: String
}